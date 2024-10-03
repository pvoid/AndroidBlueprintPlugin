/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sdk

import com.android.tools.idea.sdk.AndroidSdks
import com.intellij.execution.wsl.WslPath
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.SdkVersionUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import org.jdom.Element
import org.jetbrains.android.sdk.AndroidSdkAdditionalData
import org.jetbrains.jps.model.java.impl.JavaSdkUtil
import java.io.File
import kotlin.io.path.Path


private val JDK_PATH = "prebuilts/jdk/jdk8/linux-x86/"

private fun parsePlatformVersion(sdkHome: String): Int? {
    val file = File(sdkHome, "build/make/core/version_defaults.mk")

    val line = file.readLines().firstOrNull { line ->
        line.trimStart(' ', '\t').startsWith("PLATFORM_SDK_VERSION")
    } ?: return null

    val parts = line.split(":=")
    if (parts.size != 2) {
        return null
    }

    return try {
        parts[1].trim().toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

class AospSdkType : SdkType("AOSP JDK"), JavaSdkType {

    fun getPlatformVersion(sdk: Sdk): Int? = sdk.homePath?.let { parsePlatformVersion(it) }

    private fun toJdkPath(sdkHome: String): String {
        var path: String = FileUtil.toSystemDependentName(sdkHome)
        if (!path.endsWith(File.separator)) {
            path += File.separator
        }
        return path + JDK_PATH
    }

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean {
        val file = File(path, JDK_PATH)
        return file.exists()
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String = "API ${parsePlatformVersion(sdkHome)}"

    override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? {
        return null // TODO:
    }

    override fun getPresentableName(): String = "AOSP JDK"

    override fun getIcon() = AllIcons.Nodes.PpJdk

    override fun getBinPath(sdk: Sdk): String {
        return toJdkPath(requireNotNull(sdk.homePath)) + "bin"
    }

    override fun getToolsPath(sdk: Sdk): String {
        return toJdkPath(requireNotNull(sdk.homePath)) + "lib" + File.separator + "tools.jar"
    }

    override fun getVMExecutablePath(sdk: Sdk): String {
        val binPath = getBinPath(sdk)
        if (WslPath.isWslUncPath(binPath)) {
            return "$binPath/java"
        }
        return binPath + File.separator + "java"
    }

    override fun saveAdditionalData(data: SdkAdditionalData, element: Element) {
        if (data is AndroidSdkAdditionalData) {
            data.save(element)
        }
    }

    override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData? {
        val androidSdk = androidSdk ?: return null
        return AndroidSdkAdditionalData(androidSdk, additional)
    }

    override fun isRelevantForFile(project: Project, file: VirtualFile): Boolean {
        return PsiManager.getInstance(project).findFile(file) is PsiClassOwner
    }

    fun updateAdditionalData(sdk: Sdk, androidSdk: Sdk) {
        val sdkModificator = sdk.sdkModificator
        updateAdditionalData(sdkModificator, getPlatformVersion(sdk), androidSdk)
        WriteAction.runAndWait<Throwable> {
            sdkModificator.commitChanges()
        }
    }

    private fun updateAdditionalData(sdkModificator: SdkModificator, platformVersion: Int?, androidSdk: Sdk) {
        val sdkData = AndroidSdks.getSdkData(androidSdk)
        val target = sdkData?.targets?.firstOrNull {
            it.isPlatform && it.version.apiLevel == platformVersion
        }
        sdkModificator.sdkAdditionalData = AndroidSdkAdditionalData(androidSdk).apply {
            setBuildTarget(target)
        }
        AospSdkType.androidSdk = androidSdk
    }

    fun createJdk(jdkName: String, home: String, androidSdk: Sdk): Sdk  {
        val jdk = ProjectJdkTable.getInstance().createSdk(jdkName, INSTANCE)
        val sdkModificator = jdk.sdkModificator
        val jdkPath = toJdkPath(home)
        val platformVersion = parsePlatformVersion(home)

        sdkModificator.homePath = home
        sdkModificator.versionString = SdkVersionUtil.getJdkVersionInfo(jdkPath)?.displayVersionString()

        // Add classes
        JavaSdkUtil.getJdkClassesRoots(Path(jdkPath), false).map {
            VfsUtil.getUrlForLibraryRoot(it)
        }.sorted().forEach {
            sdkModificator.addRoot(it, OrderRootType.CLASSES)
        }
        // Add sources
        sdkModificator.addRoot(VfsUtil.getUrlForLibraryRoot(File(jdkPath, "src.zip")), OrderRootType.SOURCES)

        updateAdditionalData(sdkModificator, platformVersion, androidSdk)
        return WriteAction.computeAndWait<Sdk, Throwable> {
            sdkModificator.commitChanges()
            jdk
        }
    }

    companion object {
        private var androidSdk: Sdk? = null

        private val INSTANCE = AospSdkType()

        fun getInstance() = INSTANCE
    }
}
