/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.sdk

import com.android.tools.idea.sdk.AndroidSdks
import com.github.pvoid.androidbp.addClassesToLibrary
import com.github.pvoid.androidbp.addSourcesToLibrary
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.rpc.LOG
import java.io.File
import java.io.FileReader
import java.util.*

private val PLATFORM_VERSION_REGEX = Regex("\\s*PLATFORM_SDK_VERSION\\s*:=\\s*(\\d+)")

interface AospSdkHelper {
    fun createLibrary(libraryTable: LibraryTable, library: String, sdk: Sdk): List<Library>
    fun getLibraryBlueprint(name: String, sdk: Sdk): VirtualFile?
    fun getCachePath(blueprint: Blueprint, sdk: Sdk): File?
    fun updateAdditionalData(sdk: Sdk, indicator: ProgressIndicator)
    fun setUpAdditionalData(sdk: Sdk, modificator: SdkModificator, indicator: ProgressIndicator)

    companion object : AospSdkHelper by AospSdkHelperImpl()
}

class AospSdkHelperImpl: AospSdkHelper {

    override fun createLibrary(libraryTable: LibraryTable, library: String, sdk: Sdk): List<Library> {
        val sdkPath = sdk.homePath ?: return emptyList()
        val result = mutableListOf<Library>()
        val libraries = LinkedList<String>().apply { add(library) }
        val configured = mutableSetOf<String>()
        val sdkData = (sdk.sdkAdditionalData as? AospSdkData) ?: return emptyList()

        while (true) {
            var libraryName = libraries.pollFirst() ?: break
            // Kotlin standard library is a special case
            if (library == "kotlin-stdlib") {
                var lib = libraryTable.libraries.firstOrNull { it.name == "KotlinJavaRuntime" }
                if (lib == null) {
                    lib = libraryTable.createLibrary("KotlinJavaRuntime")
                    configureKotlinDependency(lib, sdk)
                }
                result.add(lib)
            } else {
                // Convert stubs to real link
                if (libraryName.endsWith(".stubs") && !sdkData.projects.containsKey(libraryName)) {
                    libraryName = libraryName.dropLast(6)
                }
                // Convert java aidls to a real link
                if (libraryName.endsWith("-java") && !sdkData.projects.containsKey(libraryName)) {
                    libraryName = libraryName.dropLast(5)
                }

                val blueprintFile = sdkData.projects[libraryName]?.let { File(it) } ?: continue
                val blueprint = LocalFileSystem.getInstance().findFileByIoFile(blueprintFile)?.let {
                    BlueprintsTable.get(it)
                }?.firstOrNull{ it.name == libraryName }

                if (blueprint != null) {
                    var lib = libraryTable.libraries.firstOrNull { it.name == libraryName }
                    if (lib == null) {
                        lib = libraryTable.createLibrary(libraryName)

                        lib.modifiableModel.apply {
                            configureLibrary(this, sdkPath, sdk, blueprintFile, blueprint)
                            commit()
                        }
                    }

                    (blueprint as? BlueprintWithDependencies)?.dependencies
                        ?.filterNot { configured.contains(it) || libraries.contains(it) }
                        ?.forEach(libraries::addLast)

                    result.add(lib)
                }
                configured.add(libraryName)
            }
        }

        return result
    }

    override fun getLibraryBlueprint(name: String, sdk: Sdk): VirtualFile? {
        val sdkData = (sdk.sdkAdditionalData as? AospSdkData) ?: return null
        val path = sdkData.projects[name] ?: return null
        val file = File(path)

        if (!file.exists()) {
            return null
        }

        return VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
    }

    override fun getCachePath(blueprint: Blueprint, sdk: Sdk): File? {
        val home = sdk.homePath ?: return null
        val relPath = sdk.aospSdkData?.getBlueprintFile(blueprint.name)?.parent?.path?.substring(home.length)
        return File(home, "out/soong/.intermediates/$relPath/${blueprint.name}")
    }

    override fun updateAdditionalData(sdk: Sdk, indicator: ProgressIndicator) {
        val sdkModificator = sdk.sdkModificator
        AospSdkHelper.setUpAdditionalData(sdk, sdkModificator, indicator)
        WriteAction.runAndWait<Throwable> {
            sdkModificator.commitChanges()
        }
    }

    override fun setUpAdditionalData(sdk: Sdk, modificator: SdkModificator, indicator: ProgressIndicator) {
        val root = sdk.homePath?.let { File(it) } ?: return
        val androidSdk = AndroidSdks.getInstance().allAndroidSdks.firstOrNull() ?: return
        val platformVersion = fetchPlatformVersion(root)

        indicator.text = "Collecting blueprints..."
        val blueprints = AospSdkBlueprints.findBlueprints(root)

        indicator.text = "Parsing blueprints..."
        indicator.isIndeterminate = false
        indicator.fraction = 0.0
        val step = 100.0 / blueprints.size.toFloat()
        modificator.sdkAdditionalData = blueprints.flatMap { file ->
            indicator.fraction += step
            LocalFileSystem.getInstance().findFileByIoFile(file)?.let { virtualFile ->
                try {
                    FileReader(virtualFile.path).use {
                        val factory = BlueprintCupSymbolFactory(file.parentFile)
                        BlueprintParser(BlueprintLexer(it, factory)).parse()
                        factory.blueprints
                    }
                } catch (e: RuntimeException) {
                    LOG.error("Error parsing: ${virtualFile.url}. ${e.message}", e)
                    emptyList()
                }
            }?.map {
                it.name to file.absolutePath
            } ?: emptyList()
        }.toMap().let {
            AospSdkData(sdk, androidSdk, it, platformVersion ?: 0)
        }
    }

    private fun configureKotlinDependency(library: Library, sdk: Sdk) {
        val path = File(sdk.homePath, "external/kotlinc/lib/")
        if (!path.exists() || !path.isDirectory) {
            return
        }

        library.modifiableModel.apply {
            // Classes
            File(path, "kotlin-stdlib.jar").addClassesToLibrary(this)
            File(path, "kotlin-test.jar").addClassesToLibrary(this)
            File(path, "kotlin-reflect.jar").addClassesToLibrary(this)
            File(path, "kotlin-stdlib-jdk7.jar").addClassesToLibrary(this)
            File(path, "kotlin-stdlib-jdk8.jar").addClassesToLibrary(this)
            // Sources
            File(path, "kotlin-stdlib-sources.jar").addSourcesToLibrary(this)
            File(path, "kotlin-test-sources.jar").addClassesToLibrary(this)
            File(path, "kotlin-reflect-sources.jar").addSourcesToLibrary(this)
            File(path, "kotlin-stdlib-jdk7-sources.jar").addSourcesToLibrary(this)
            File(path, "kotlin-stdlib-jdk8-sources.jar").addSourcesToLibrary(this)

            commit()
        }
    }

    private fun configureLibrary(library: Library.ModifiableModel, sdkPath: String, sdk: Sdk, blueprintFile: File, blueprint: Blueprint) {
        val filesManager = VirtualFileManager.getInstance()
        BlueprintHelper.collectBlueprintSources(blueprint, sdk).map(File::toFileSystemUrl).forEach { url ->
            filesManager.findFileByUrl(url)?.let {
                library.addRoot(it, OrderRootType.SOURCES)
            }
        }

        getBlueprintArtifacts(sdkPath, blueprint, blueprintFile).forEach {
            it.addClassesToLibrary(library)
        }
    }

    private fun getBlueprintArtifacts(sdkPath: String, blueprint: Blueprint, blueprintFile: File): List<File> {
        if (blueprint !is BlueprintWithArtifacts) {
            return emptyList()
        }
        if (!blueprintFile.parentFile.absolutePath.startsWith(sdkPath)) {
            return emptyList()
        }

        val relPath = blueprintFile.parentFile.absolutePath.substring(sdkPath.length)
        val path = File(sdkPath, "out/soong/.intermediates/$relPath/${blueprint.name}")
        return blueprint.getArtifacts(path)
    }

    private fun fetchPlatformVersion(root: File): Int? {
        return FileReader(File(root, "build/make/core/version_defaults.mk")).use { file ->
            val text = file.readText()
            val gropus = PLATFORM_VERSION_REGEX.find(text)?.groupValues
            if (gropus?.size == 2) {
                gropus[1].toInt()
            } else {
                null
            }
        }
    }
}

val Sdk.aospSdkData: AospSdkData?
    get() = sdkAdditionalData as? AospSdkData