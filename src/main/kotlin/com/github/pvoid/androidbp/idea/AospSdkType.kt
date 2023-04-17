/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea

import com.android.tools.idea.sdk.AndroidSdks
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaDependentSdkType
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.systemIndependentPath
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import org.jdom.Element
import org.jetbrains.android.sdk.AndroidSdkAdditionalData
import org.jetbrains.jps.model.java.impl.JavaSdkUtil
import java.io.File
import java.nio.file.Path

class AospSdkType : JavaDependentSdkType("AOSP") {

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean {
        return File(path, "prebuilts/jdk/jdk8/linux-x86").exists() &&
                File(path, "build/make/core/version_defaults.mk").exists()
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val name = currentSdkName ?: "AOSP Source Code"
        val version = parsePlatformVersion(File(sdkHome)) ?: return name

        return "$name API LEVEL $version"
    }

    override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
        // Nothing really to save, but we need to do some custom trick on restore
    }

    override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData? {
        ApplicationManager.getApplication().invokeLater {
            val androidSdk = AndroidSdks.getInstance().allAndroidSdks.firstOrNull()
            if (androidSdk != null) {
                currentSdk.sdkModificator.apply {
                    sdkAdditionalData = AndroidSdkAdditionalData(androidSdk, currentSdk)
                    commitChanges()
                }
            }
        }
        return null
    }

    override fun setupSdkPaths(sdk: Sdk, sdkModel: SdkModel): Boolean {
        val homePath = sdk.homePath ?: return false

        sdk.sdkModificator.apply {
            val jdkHome = Path.of(homePath, "prebuilts/jdk/jdk8/linux-x86")
            // Add classes
            JavaSdkUtil.getJdkClassesRoots(jdkHome, false).map {
                VfsUtil.getUrlForLibraryRoot(it.toFile())
            }.forEach {
                addRoot(it, OrderRootType.CLASSES)
            }
            // Add sources
            val jdkSrc = File(sdk.homePath, "prebuilts/jdk/jdk8/linux-x86/src.zip")
            if (jdkSrc.exists()) {
                sequenceOf("src", "").mapNotNull {
                    val url = JarFileSystem.PROTOCOL_PREFIX + jdkSrc.systemIndependentPath + JarFileSystem.JAR_SEPARATOR + it
                    VirtualFileManager.getInstance().findFileByUrl(url)
                }.firstOrNull()?.let {
                    addRoot(it, OrderRootType.SOURCES)
                }
            }
            JavaSdkImpl.attachIDEAAnnotationsToJdk(this)
            commitChanges()
        }
        return true
    }

    override fun getPresentableName(): String = "Android Opensource Project Root Folder"

    override fun getBinPath(sdk: Sdk): String = JavaSdk.getInstance().getBinPath(sdk)

    override fun getToolsPath(sdk: Sdk): String = JavaSdk.getInstance().getToolsPath(sdk)

    override fun getVMExecutablePath(sdk: Sdk): String = JavaSdk.getInstance().getVMExecutablePath(sdk)

    companion object {
        val INSTANCE = AospSdkType()

        fun parsePlatformVersion(path: File): Int? {
            val file = File(path, "build/make/core/version_defaults.mk")

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
    }
}