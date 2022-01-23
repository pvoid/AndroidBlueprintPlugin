/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module

import android.content.res.BridgeAssetManagerExt
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.util.toIoFile
import com.github.pvoid.androidbp.LOG
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.android.AospAndroidModel
import com.github.pvoid.androidbp.module.sdk.*
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.facet.FacetManager
import com.intellij.facet.ModifiableFacetModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.android.facet.*
import org.jetbrains.android.sdk.StudioEmbeddedRenderTarget
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.io.File

interface AospProjectHelper {

    fun blueprintFileUrlForProject(project: Project): String?

    fun blueprintFileForProject(project: Project): VirtualFile?

    fun checkAndAssignSdk(project: Project, indicator: ProgressIndicator, canCreateSdk: Boolean = false): Sdk?

    fun createDependencies(project: Project, sdk: Sdk): List<Library>

    fun addDependencies(project: Project, libraries: List<Library>)

    fun addFacets(project: Project, sdk: Sdk)

    fun addSourceRoots(project: Project, sdk: Sdk)

    fun shouldHaveFacet(blueprint: Blueprint): Boolean

    fun updateSourceRoots(project: Project, blueprints: List<Blueprint>)

    fun fixLayoutLibrary(project: Project, sdk: Sdk)

    companion object : AospProjectHelper by AospProjectHelperImpl()
}

private class AospProjectHelperImpl : AospProjectHelper {

    override fun blueprintFileUrlForProject(project: Project): String? {
        val file = project.basePath?.let { File(it) } ?: return null
        return file.toFileSystemUrl() + "Android.bp"
    }

    override fun blueprintFileForProject(project: Project): VirtualFile? {
        val file = project.basePath?.let { File(it, "Android.bp") } ?: return null
        if (!file.exists()) {
            return null
        }
        return LocalFileSystem.getInstance().findFileByPath(file.absolutePath)
    }

    override fun checkAndAssignSdk(project: Project, indicator: ProgressIndicator, canCreateSdk: Boolean): Sdk? {
        val manager = ProjectRootManager.getInstance(project)
        val sdk = manager.projectSdk
        // Check if SDK is AOSP source
        if (sdk?.sdkType?.name != AOSP_SDK_TYPE_NAME) {
            // Looking for the first AOSP sdk
            var newSdk = ProjectJdkTable.getInstance().allJdks.firstOrNull { it.sdkType is AospSdkType }

            if (newSdk == null && canCreateSdk) {
                newSdk = project.basePath?.let { AospSdkHelper.tryToCreateSdk(it) }
            }

            newSdk?.let {
                if (ApplicationManager.getApplication().isWriteAccessAllowed) {
                    manager.projectSdk = it
                } else {
                    WriteAction.runAndWait<Throwable> {
                        manager.projectSdk = it
                    }
                }
                return it
            }
        } else if (sdk.sdkType.name == AOSP_SDK_TYPE_NAME) {
            if (sdk.aospSdkData?.isOld() == true) {
                AospSdkHelper.updateAdditionalData(sdk, indicator)
            }
            return sdk
        }

        return null
    }

    override fun createDependencies(project: Project, sdk: Sdk): List<Library> {
        val blueprintFile = blueprintFileForProject(project) ?: return emptyList()

        val blueprints = BlueprintsTable.get(blueprintFile)
        val dependencies = blueprints.flatMap { blueprint ->
            if (blueprint is BlueprintWithDependencies) {
                blueprint.dependencies
            } else {
                emptyList()
            }
        }
        val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        return WriteAction.computeAndWait<List<Library>, Throwable> {
            dependencies.flatMap {
                AospSdkHelper.createLibrary(libraryTable, it, sdk)
            }.toList()
        }
    }

    override fun addDependencies(project: Project, libraries: List<Library>) {
        val blueprintFile = blueprintFileForProject(project) ?: return
        val module = ReadAction.compute<Module?, Throwable> {
            ModuleUtil.findModuleForFile(blueprintFile, project)
        } ?: return

        val model = ReadAction.compute<ModifiableRootModel, Throwable> {
            ModuleRootManager.getInstance(module).modifiableModel
        }

        WriteAction.runAndWait<Throwable> {
            model.orderEntries.mapNotNull { it as? LibraryOrderEntry }.forEach { current ->
                if (!libraries.any { it.name == current.libraryName }) {
                    model.removeOrderEntry(current)
                }
            }

            libraries.mapNotNull {
                if (model.findLibraryOrderEntry(it) == null) {
                    model.addLibraryEntry(it)
                } else {
                    null
                }
            }.forEach {
                it.isExported = true
                it.scope = DependencyScope.COMPILE
            }

            model.commit()
        }
    }

    override fun addFacets(project: Project, sdk: Sdk) {
        val blueprintFile = blueprintFileForProject(project) ?: return
        val module = ReadAction.compute<Module?, Throwable> {
            ModuleUtil.findModuleForFile(blueprintFile, project)
        } ?: return
        val blueprints = BlueprintsTable.get(blueprintFile)
        val androidApps = blueprints.filter(::shouldHaveFacet)

        if (androidApps.isEmpty()) {
            return
        }

        val facetManager = FacetManager.getInstance(module)
        val model = ReadAction.compute<ModifiableFacetModel, Throwable> {
            facetManager.createModifiableModel()
        }

        WriteAction.runAndWait<Throwable> {
            val androidFacetType = AndroidFacetType()
            val updatedFacets = androidApps.map { blueprint ->
                val facet: AndroidFacet? = facetManager.findFacet(AndroidFacet.ID, blueprint.name)
                if (facet != null) {
                    blueprint to facet
                } else {
                    val config = AndroidFacetConfiguration()
                    config.state.COMPILE_CUSTOM_GENERATED_SOURCES = false
                    config.state.GEN_FOLDER_RELATIVE_PATH_APT = "/.idea/cache/gen/"
                    config.state.GEN_FOLDER_RELATIVE_PATH_AIDL = "/.idea/cache/gen/"
                    config.state.ALLOW_USER_CONFIGURATION = false

                    blueprint to androidFacetType.createFacet(module, blueprint.name, config, null).also(model::addFacet)
                }
            }.mapNotNull { (blueprint, facet) ->
                AndroidModel.set(facet, AospAndroidModel(project, blueprint))
                if (cleanUpFacet(facet)) {
                    facet
                } else {
                    null
                }
            }

            model.commit()

            updatedFacets.forEach(facetManager::facetConfigurationChanged)
        }
    }

    override fun addSourceRoots(project: Project, sdk: Sdk) {
        val blueprintFile = blueprintFileForProject(project) ?: return
        val module = ReadAction.compute<Module?, Throwable> {
            ModuleUtil.findModuleForFile(blueprintFile, project)
        } ?: return

        val model = ReadAction.compute<ModifiableRootModel, Throwable> {
            ModuleRootManager.getInstance(module).modifiableModel
        }

        val blueprints = BlueprintsTable.get(blueprintFile)
        val resources = blueprints.filterIsInstance<BlueprintWithResources>().flatMap {
            BlueprintHelper.collectBlueprintResources(it, sdk)
        }
        val sources = blueprints.filterIsInstance<BlueprintWithSources>().flatMap {
            BlueprintHelper.collectBlueprintSources(it, sdk, false)
        }.asSequence().mapNotNull {
            if (it.isFile) it.parentFile else it
        }.sortedBy {
            it.absolutePath
        }.fold(mutableListOf<File>()) { acc, item ->
            val last = acc.lastOrNull()
            if (item.exists() && (last == null || !FileUtil.isAncestor(last, item, false))) {
                acc.add(item)
            }
            acc
        }.mapNotNull { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }

        WriteAction.runAndWait<Throwable> {
            addResourcesRootFolder(model, resources)
            addSourceRootFolder(model, sources)
            model.commit()
        }
    }

    override fun shouldHaveFacet(blueprint: Blueprint): Boolean =
        blueprint is AndroidAppBlueprint || blueprint is AndroidLibraryBlueprint
                || blueprint is JavaSdkLibraryBlueprint || blueprint is AidlJavaInterfaceBlueprint

    override fun updateSourceRoots(project: Project, blueprints: List<Blueprint>) {
        WriteAction.runAndWait<Throwable> {
            project.modifyModules {
                val baseFile = File(project.basePath)
                val module = this.modules.firstOrNull { module ->
                    ModuleType.get(module).id == ModuleTypeId.JAVA_MODULE
                } ?: return@modifyModules
                ModuleRootManager.getInstance(module).modifiableModel.also { model ->
                    val dirs = blueprints.flatMap { blueprint ->
                        when (blueprint) {
                            is BlueprintWithAidls -> blueprint.aidls
                            is BlueprintWithSources -> blueprint.sources
                            else -> null
                        }
                    }.mapNotNull {
                        (it as? GlobItem)?.toFullPath(baseFile)?.toVirtualFile()
                    }
                    addSourceRootFolder(model, dirs)
                    model.commit()
                }
            }
        }
    }

    override fun fixLayoutLibrary(project: Project, sdk: Sdk) {
        val data = (sdk.sdkAdditionalData as AospSdkData).androidSdkData
        val loaded = data?.targets?.any { target ->
            try {
                val compatTarget = StudioEmbeddedRenderTarget.getCompatibilityTarget(target)
                data.getTargetData(compatTarget).getLayoutLibrary(project)
                true
            } catch (e: UnsatisfiedLinkError) {
                LOG.error("Can't load layout library", e)
                false
            } catch (e: Throwable) {
                LOG.error("Can't load layout library", e)
                false
            }
        } ?: false

        if (loaded && isAssetsBridgeLoaded()) {
            BridgeAssetManagerExt.init(sdk.homePath!!);
        }
    }

    private fun isAssetsBridgeLoaded(): Boolean = try {
        Class.forName("android.content.res.BridgeAssetManager")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    private fun addSourceRootFolder(model: ModifiableRootModel, srcs: List<VirtualFile>) {
        srcs.forEach { path ->
            model.contentEntries.firstOrNull { entry ->
                val root = entry.file?.toIoFile() ?: return@firstOrNull false
                val src = path.toIoFile()
                FileUtil.isAncestor(root, src, false)
            }?.addSourceFolder(path, false)
        }
    }

    private fun addResourcesRootFolder(model: ModifiableRootModel, resources: List<VirtualFile>) {
        val sourceType = JavaResourceRootType.RESOURCE
        val entry = model.contentEntries.first()
        resources.forEach {
            entry.addSourceFolder(it, sourceType)
        }
    }

    private fun cleanUpFacet(facet: AndroidFacet): Boolean {
        var updated = false;

        if (facet.properties.RES_FOLDER_RELATIVE_PATH != null) {
            facet.properties.RES_FOLDER_RELATIVE_PATH = null
            updated = true
        }

        if (facet.properties.ASSETS_FOLDER_RELATIVE_PATH != null) {
            facet.properties.ASSETS_FOLDER_RELATIVE_PATH = null
            updated = true
        }

        if (facet.properties.RES_FOLDERS_RELATIVE_PATH != null) {
            facet.properties.RES_FOLDERS_RELATIVE_PATH = null
            updated = true
        }

        if (facet.properties.MANIFEST_FILE_RELATIVE_PATH != null) {
            facet.properties.MANIFEST_FILE_RELATIVE_PATH = null
            updated = true
        }

        return updated;
    }
}
