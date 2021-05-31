/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module

import com.android.AndroidProjectTypes
import com.android.tools.idea.model.AndroidModel
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.android.AospAndroidModel
import com.github.pvoid.androidbp.module.sdk.AOSP_SDK_TYPE_NAME
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.AospSdkType
import com.github.pvoid.androidbp.module.sdk.aospSdkData
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.facet.FacetManager
import com.intellij.facet.ModifiableFacetModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.facet.*
import org.jetbrains.jps.model.java.JavaResourceRootType
import java.io.File

interface AospProjectHelper {

    fun blueprintFileUrlForProject(project: Project): String?

    fun blueprintFileForProject(project: Project): VirtualFile?

    fun checkAndAssignSdk(project: Project, indicator: ProgressIndicator): Sdk?

    fun createDependencies(project: Project, sdk: Sdk): List<Library>

    fun addDependencies(project: Project, libraries: List<Library>)

    fun addFacets(project: Project, sdk: Sdk)

    fun addSourceRoots(project: Project, sdk: Sdk)

    fun updateFacet(sdk: Sdk, blueprint: Blueprint, facet: AndroidFacet)

    fun shouldHaveFacet(blueprint: Blueprint): Boolean

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

    override fun checkAndAssignSdk(project: Project, indicator: ProgressIndicator): Sdk? {
        val manager = ProjectRootManager.getInstance(project)
        val sdk = manager.projectSdk
        // Check if SDK is AOSP source
        if (sdk?.sdkType?.name != AOSP_SDK_TYPE_NAME) {
            // Looking for the first AOSP sdk
            ProjectJdkTable.getInstance().allJdks.firstOrNull { it.sdkType is AospSdkType }?.let { foundSdk ->
                if (ApplicationManager.getApplication().isWriteAccessAllowed) {
                    manager.projectSdk = foundSdk
                } else {
                    WriteAction.runAndWait<Throwable> {
                        manager.projectSdk = foundSdk
                    }
                }
                return foundSdk
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

        val facets = FacetManager.getInstance(module)
        val model = ReadAction.compute<ModifiableFacetModel, Throwable> {
            facets.createModifiableModel()
        }

        WriteAction.runAndWait<Throwable> {
            val androidFacet = AndroidFacetType()
            androidApps.map { blueprint ->
                val facet: AndroidFacet? = facets.findFacet(AndroidFacet.ID, blueprint.name)
                if (facet != null) {
                    blueprint to facet
                } else {
                    val config = AndroidFacetConfiguration()
                    config.state.COMPILE_CUSTOM_GENERATED_SOURCES = false
                    config.state.GEN_FOLDER_RELATIVE_PATH_APT = "/.idea/cache/gen/"
                    config.state.GEN_FOLDER_RELATIVE_PATH_AIDL = "/.idea/cache/gen/"
                    config.state.ALLOW_USER_CONFIGURATION = false

                    blueprint to androidFacet.createFacet(module, blueprint.name, config, null).also(model::addFacet)
                }
            }.forEach { (blueprint, facet) ->
                updateFacet(sdk, blueprint, facet)
                AndroidModel.set(facet, AospAndroidModel(project, blueprint))
            }

            model.commit()
        }
    }

    override fun updateFacet(sdk: Sdk, blueprint: Blueprint, facet: AndroidFacet) {
        facet.properties.PROJECT_TYPE =
            if (blueprint is AndroidAppBlueprint) AndroidProjectTypes.PROJECT_TYPE_APP else AndroidProjectTypes.PROJECT_TYPE_LIBRARY

        val localRes = (blueprint as? BlueprintWithResources)?.resources?.firstOrNull() as? GlobItem
        facet.properties.RES_FOLDER_RELATIVE_PATH =  localRes?.toRelativeString()

        val localAssets = (blueprint as? BlueprintWithAssets)?.assets?.firstOrNull() as? GlobItem
        facet.properties.ASSETS_FOLDER_RELATIVE_PATH = localAssets?.toRelativeString()

        val resources = BlueprintHelper.collectBlueprintResources(blueprint, sdk)
        facet.properties.RES_FOLDERS_RELATIVE_PATH = resources.joinToString(
            AndroidFacetProperties.PATH_LIST_SEPARATOR_IN_FACET_CONFIGURATION
        ) { it.url }

        facet.properties.MANIFEST_FILE_RELATIVE_PATH = (blueprint as? BlueprintWithManifest)?.manifest?.let {
            if (it.startsWith("/")) it else "/$it"
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

        WriteAction.runAndWait<Throwable> {
            addSourceRootFolder(model, resources)
            model.commit()
        }
    }

    override fun shouldHaveFacet(blueprint: Blueprint): Boolean =
        blueprint is AndroidAppBlueprint || blueprint is AndroidLibraryBlueprint || blueprint is JavaSdkLibraryBlueprint

    private fun addSourceRootFolder(model:  ModifiableRootModel, resources: List<VirtualFile>) {
        val sourceType = JavaResourceRootType.RESOURCE
        val entry = model.contentEntries.first()
        resources.forEach {
            entry.addSourceFolder(it, sourceType)
        }
    }
}
