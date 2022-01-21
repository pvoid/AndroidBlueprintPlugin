/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.sdk

import com.android.tools.idea.sdk.AndroidSdks
import com.github.pvoid.androidbp.LOG
import com.github.pvoid.androidbp.ManifestInfo
import com.github.pvoid.androidbp.getSystemIndependentPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.JavaDependentSdkType
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.*
import org.jdom.Element
import org.jetbrains.jps.model.java.impl.JavaSdkUtil
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import java.util.*
import javax.swing.SwingUtilities

const val AOSP_SDK_TYPE_NAME = "AOSP"

@Suppress("NAME_SHADOWING")
class AospSdkType : JavaDependentSdkType(AOSP_SDK_TYPE_NAME) {

    private val mShowAndroidSdkErrorRunnable = Runnable {
        Messages.showErrorDialog(
            "Android SDK is required to support resource editing and resolving. Please create any Android SDK first and try again",
            "Can't Create AOSP SDK"
        )
    }

    private val isLinux: Boolean by lazy {
        System.getProperty("os.name").lowercase(Locale.ENGLISH).contains("linux")
    }

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
        if (additionalData is AospSdkData) {
            additionalData.save(additional)
        }
    }

    override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData? {
        AospSdkData.read(currentSdk, additional)?.let {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.run<Throwable> {
                    it(currentSdk.sdkModificator)
                }
            }
        }
        return null
    }

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean {
        if (!isLinux) {
            Messages.showErrorDialog("Only Linux is supported", "Can't Create AOSP SDK")
            return false
        }

        val repoFolder = File(path, ".repo")
        if (!repoFolder.exists() || !repoFolder.isDirectory) {
            return false
        }

        val manifest = File(repoFolder, "manifest.xml")

        if (!manifest.exists() || !manifest.isFile) {
            return false
        }

        if (AndroidSdks.getInstance().allAndroidSdks.isEmpty()) {
            ApplicationManager.getApplication().also { app ->
                when {
                    app.isDispatchThread -> mShowAndroidSdkErrorRunnable.run()
                    app.isWriteAccessAllowed -> app.runWriteAction(mShowAndroidSdkErrorRunnable)
                    else -> SwingUtilities.invokeAndWait {
                        app.runWriteAction(mShowAndroidSdkErrorRunnable)
                    }
                }
            }
            return false
        }

        return true
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val info = parseManifestInfo(sdkHome)
        return "AOSP Source Code${if (info == null) "" else " (${info.reference})"}"
    }

    override fun getPresentableName(): String = "Android Opensource Project Root Folder"

    override fun setupSdkPaths(sdk: Sdk) {
        val sdkModificator = sdk.sdkModificator
        setUpJdk(sdk, sdkModificator)
        val scanTask = object : Task.ConditionalModal(null, "Indexing blueprints", false, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                AospSdkHelper.setUpAdditionalData(sdk, sdkModificator, indicator)
                WriteAction.runAndWait<Throwable> {
                    sdkModificator.commitChanges()
                }
            }
        }

        ProgressManager.getInstance().run(scanTask)
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator
    ): AdditionalDataConfigurable? {
        return null // AndroidSdkConfigurable(sdkModel, sdkModificator)
    }

    private fun setUpJdk(sdk: Sdk, modificator: SdkModificator) {
        val jdkHome = Path.of(sdk.homePath, "prebuilts/jdk/jdk8/linux-x86")

        // Add classes
        JavaSdkUtil.getJdkClassesRoots(jdkHome, false).map {
            VfsUtil.getUrlForLibraryRoot(it.toFile())
        }.forEach {
            modificator.addRoot(it, OrderRootType.CLASSES)
        }
        // Add sources
        val jdkSrc = File(sdk.homePath, "prebuilts/jdk/jdk8/linux-x86/src.zip")
        if (jdkSrc.exists()) {
            (findInJar(jdkSrc, "src") ?: findInJar(jdkSrc, ""))?.let {
                modificator.addRoot(it, OrderRootType.SOURCES)
            }
        }

        JavaSdkImpl.attachIDEAAnnotationsToJdk(modificator)
    }

    override fun getVersionString(sdk: Sdk): String? = parseManifestInfo(sdk.homePath)?.reference

    override fun getBinPath(sdk: Sdk): String = JavaSdk.getInstance().getBinPath(sdk)

    override fun getToolsPath(sdk: Sdk): String = JavaSdk.getInstance().getToolsPath(sdk)

    override fun getVMExecutablePath(sdk: Sdk): String = JavaSdk.getInstance().getVMExecutablePath(sdk)

    private fun parseManifestInfo(path: String?): ManifestInfo? {
        val repo = File(path, ".repo")
        if (!repo.exists() || !repo.isDirectory) {
            return null
        }

        val manifest = File(repo, "manifest.xml")
        if (!manifest.exists() || !manifest.isFile) {
            return null
        }

        return FileInputStream(manifest).use { it ->
            try {
                ManifestInfo.read(it)
            } catch (e: IOException) {
                LOG.error(e)
                null
            }
        }
    }

    private fun findInJar(jarFile: File, relativePath: String): VirtualFile? {
        if (!jarFile.exists()) {
            return null
        }
        val url =
            JarFileSystem.PROTOCOL_PREFIX + jarFile.getSystemIndependentPath() + JarFileSystem.JAR_SEPARATOR + relativePath
        return VirtualFileManager.getInstance().findFileByUrl(url)
    }

    companion object {
        val INSTANCE = AospSdkType()
    }
}
