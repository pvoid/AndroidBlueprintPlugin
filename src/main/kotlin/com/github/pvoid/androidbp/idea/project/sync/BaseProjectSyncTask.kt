/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sync

import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.util.toVirtualFile
import com.github.pvoid.androidbp.blueprint.*
import com.github.pvoid.androidbp.idea.LOG
import com.github.pvoid.androidbp.idea.project.*
import com.intellij.facet.FacetManager
import com.intellij.facet.ModifiableFacetModel
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.AndroidFacetProperties
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.io.File
import java.util.*

internal abstract class BaseProjectSyncTask(
    project: Project,
    title: String
) : Task.ConditionalModal(project, title, false, ALWAYS_BACKGROUND) {

    private val blueprintFile: File? by lazy {
        project.getProjectBlueprint()
    }

    private val katiMakefile: File? by lazy {
        project.getProjectMakefile()
    }

    protected val module: Module? by lazy {
        (blueprintFile ?: katiMakefile)?.toVirtualFile()?.let {
            ModuleUtil.findModuleForFile(it, project)
        }
    }

    protected val blueprints: List<Blueprint> by lazy {
        val table = BlueprintsTable.getInstance(project)
        val bp = blueprintFile
        val mk = katiMakefile
        when {
            bp != null -> table.parse(bp)
            mk != null -> project.guessAospRoot()?.let { Makefile.parse(mk, it) } ?: emptyList()
            else -> emptyList()
        }
    }

    protected fun collectBlueprintFiles(root: File): List<File> = collectFiles(root, Blueprint.DEFAULT_NAME)

    protected fun collectKatiFiles(root: File): List<File> = collectFiles(root, Makefile.DEFAULT_NAME)

    private fun collectFiles(root: File, name: String): List<File> {
        val folders = Stack<File>().apply {
            push(root)
        }
        val result = mutableListOf<File>()

        while (folders.isNotEmpty()) {
            folders.pop().listFiles()?.forEach { entry ->
                if (entry.isFile) {
                    if (entry.name == name) {
                        result.add(entry)
                    }
                } else if (entry.isDirectory) {
                    // Skip hidden and service folders
                    if (!entry.name.startsWith(".")) {
                        folders.push(entry)
                    }
                }
            }
        }
        return result
    }

    protected fun parseBlueprints(indicator: ProgressIndicator, aospRoot: File, files: List<File>): Map<String, File> {
        val result = mutableMapOf<String, File>()
        val ext = mutableListOf<File>()
        val queue = Stack<File>()
        var total = files.size
        var processed = 0
        queue.addAll(files)

        indicator.isIndeterminate = false
        indicator.fraction = 0.0

        while (queue.isNotEmpty()) {
            val file = queue.pop()
            ext.clear()
            BlueprintsTable.parse(aospRoot, file, ext).forEach { blueprint ->
                result[blueprint.name] = file
            }
            queue.addAll(ext)
            total += ext.size
            ++processed

            indicator.fraction = processed.toDouble() / total
        }

        return result
    }

    protected fun parseMakefiles(indicator: ProgressIndicator, aospRoot: File, files: List<File>): Map<String, File> {
        var processed = 0

        return files.flatMap { file ->
            Makefile.parse(file, aospRoot).map {
                it.name to file
            }.also {
                ++processed
                indicator.fraction = processed.toDouble() / files.size
            }
        }.toMap()
    }

    protected fun updateProjectBlueprints(): List<Blueprint> {
        val moduleSystem = module?.getModuleSystem() as? BlueprintModuleSystem
        moduleSystem?.updateBlueprints(blueprints)
        return blueprints
    }

    protected fun updateProjectFacets(): List<AndroidFacet> {
        val facetsManager = module?.let(FacetManager::getInstance) ?: return emptyList()
        val facetsModel = facetsManager.createModifiableModel()

        return WriteAction.computeAndWait<List<AndroidFacet>, Throwable> {
            blueprints.filter {
                it.type == BlueprintType.AndroidApp || it.type == BlueprintType.AndroidLibrary
            }.mapNotNull { blueprint ->
                val facet = facetsManager.getFacetsByType(AndroidFacet.ID).firstOrNull { facet ->
                    facet.name == blueprint.name
                } ?: module?.let {
                    createAndroidFacet(it, facetsModel, blueprint.name)
                }
                facet?.let {
                    blueprint to it
                }
            }.map { (blueprint, facet) ->
                // Despite all deprecation the property is used to build up local resources
                // cache
                facet.properties.RES_FOLDER_RELATIVE_PATH = blueprint.resources(false).joinToString(";") { file ->
                    VfsUtilCore.pathToUrl(file)
                }
                facetsManager.facetConfigurationChanged(facet)
                facet
            }.also { facets ->
                facetsModel.getFacetsByType(AndroidFacet.ID).filterNot { facet ->
                    facets.any { it.name == facet.name }
                }.forEach(facetsModel::removeFacet)

                facetsModel.commit()
            }
        }
    }

    private fun createAndroidFacet(module: Module, facetModel: ModifiableFacetModel, name: String): AndroidFacet {
        val facetType = AndroidFacet.getFacetType()
        val configuration = facetType.createDefaultConfiguration()

        @Suppress("DEPRECATION")
        configuration.state.ALLOW_USER_CONFIGURATION = false
        val facet = facetType.createFacet(module, name, configuration, null)
        facetModel.addFacet(facet)
        return facet
    }

    protected fun updateJavaDependencies(aospRoot: File) {
        val model = module?.let {
            ModuleRootManager.getInstance(it).modifiableModel
        } ?: return
        val dependencies = mutableMapOf<String, Blueprint>()
        val queue = Stack<String>()
        val table = BlueprintsTable.getInstance(project)

        blueprints.flatMap { it.dependencies(DependenciesScope.All) }.toCollection(queue)
        blueprints.flatMap { it.defaults() }.toCollection(queue)

        while (queue.isNotEmpty()) {
            val name = queue.pop()

            if (dependencies.containsKey(name)) {
                continue
            }

            val blueprint = table[name]

            if (blueprint != null) {
                dependencies[name] = blueprint
                blueprint.dependencies(DependenciesScope.Dynamic).toCollection(queue)
                blueprint.defaults().toCollection(queue)
            } else {
                LOG.warn("Dependency $name is not found")
            }
        }

        // Not all dependencies have the same name as dependency
        // It is possible to fix up the name first and then remove
        // disappeared dependencies, but easier remove first and then
        // add existing
        model.orderEntries
            .filterIsInstance(LibraryOrderEntry::class.java)
            .filterNot {
                dependencies.containsKey(it.libraryName)
            }.forEach(model::removeOrderEntry)

        val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        // Drop old libs
        dependencies.values.mapNotNull { blueprint ->
            LibrariesTools.createLibrary(libraryTable, blueprint, table, aospRoot)
        }.plus(
            blueprints.mapNotNull { blueprint ->
                LibrariesTools.createAidlGenLibrary(project, libraryTable, blueprint)
            }
        ).map { lib ->
            model.findLibraryOrderEntry(lib) ?: model.addLibraryEntry(lib)
        }.forEach {
            it.isExported = true
            it.scope = DependencyScope.COMPILE
        }

        WriteAction.runAndWait<Throwable> {
            model.commit()
        }
    }

    protected fun updateAndroidDependencies(aospRoot: File) {
        val table = BlueprintsTable.getInstance(project)
        val dependencies = mutableMapOf<String, AndroidDependencyRecord>()
        val outputPath = LibrariesTools.getOutputPath(aospRoot)
        val queue = blueprints.flatMap {
            it.dependencies(DependenciesScope.All)
        }.mapNotNull(table::get)
        .filter { it.type == BlueprintType.AndroidApp || it.type == BlueprintType.AndroidLibrary }
        .foldRight(mutableListOf<Blueprint>()) { blueprint, queue ->
            queue.add(blueprint)
            queue
        }

        while (queue.isNotEmpty()) {
            val blueprint = queue.removeAt(0)
            val record = AndroidDependencyRecord.Builder(blueprint.name)
            val deps = blueprint.dependencies(DependenciesScope.All)
                .filterNot(dependencies::containsKey)
                .mapNotNull(table::get)


            deps.filter { it.type == BlueprintType.AndroidApp || it.type == BlueprintType.AndroidLibrary }.forEach(queue::add)

            blueprint.packageName()?.let(record::withPackageName)
            blueprint.R()?.let(record::withR)
            blueprint.assets().foldRight(record) { assets, r ->
                r.withAssets(assets)
            }
            blueprint.resources(false).foldRight(record) { res, r ->
                r.withRes(res)
            }
            blueprint.manifest()?.let(record::withManifest)
            blueprint.outputJars(outputPath).forEach(record::withJar)

            deps.filter { it.type == BlueprintType.AndroidImport }.forEach { lib ->
                lib.generatedResources().foldRight(record) { res, r ->
                    r.withGeneratedRes(res)
                }
                lib.resApk()?.let(record::withResApk)
                lib.outputJars(outputPath).forEach(record::withJar)
            }

            dependencies[blueprint.name] = record.build()
        }

        val moduleSystem = module?.getModuleSystem() as? BlueprintModuleSystem ?: return
        moduleSystem.updateAndroidDependencies(dependencies.values)
    }

    protected fun updateSourceRoots() {
        val model = module?.let {
            ReadAction.compute<ModifiableRootModel, Throwable> {
                ModuleRootManager.getInstance(it).modifiableModel
            }
        } ?: return
        val entry = model.contentEntries.first()
        WriteAction.runAndWait<Throwable> {
            // Drop existing entries
            entry.sourceFolders.filter {
                it.rootType == JavaResourceRootType.RESOURCE || it.rootType == JavaSourceRootType.SOURCE
            }.forEach {
                entry.removeSourceFolder(it)
            }

            // resources
            blueprints.flatMap {
                it.resources(false)
            }.mapNotNull {
                File(it).toVirtualFile()
            }.forEach { path ->
                entry.addSourceFolder(path, JavaResourceRootType.RESOURCE)
            }

            // source code
            blueprints.flatMap {
                it.sources(false)
            }.mapNotNull {
                File(it).toVirtualFile()
            }.forEach { path ->
                entry.addSourceFolder(path, JavaSourceRootType.SOURCE)
            }

            model.commit()
        }
    }
}
