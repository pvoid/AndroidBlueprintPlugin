/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.sdk.AospSdkData
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.aospSdkData
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.backend.common.pop
import java.io.File
import java.util.*

private val LIBRARY_TYPES = listOf("java", "kt")

interface BlueprintHelper {
    fun collectBlueprintResources(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile>
    fun collectBlueprintAssets(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile>
    fun collectBlueprintAidls(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile>
    fun getBlueprintManifest(blueprint: Blueprint, sdk: Sdk): VirtualFile?
    fun getBlueprintR(blueprint: Blueprint, sdk: Sdk): VirtualFile?
    fun getBlueprintResApk(blueprint: Blueprint, sdk: Sdk): VirtualFile?
    fun collectBlueprintSources(blueprint: Blueprint, sdk: Sdk, includeDynamic: Boolean): Set<File>
    fun collectGeneratedSources(blueprint: Blueprint, sdk: Sdk): Set<File>
    fun collectBlueprintDependencies(blueprint: Blueprint, sdk: Sdk): List<Blueprint>

    companion object : BlueprintHelper by BlueprintHelperImpl()
}

class BlueprintHelperImpl : BlueprintHelper {
    override fun collectBlueprintResources(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile> {
        val sdkData = sdk.aospSdkData ?: return emptySet()
        val path = AospSdkHelper.getCachePath(blueprint, sdk) ?: return emptySet()
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return emptySet()

        val result = collectBlueprintSources(sdkData, blueprintFile, blueprint) {
            (this as? BlueprintWithResources)?.resources ?: emptyList()
        }.mapNotNull { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }.toMutableSet()

        if (blueprint is BlueprintWithDynamicResources) {
            blueprint.getResources(path).let { file ->
                VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
            }?.let(result::add)
        }

        return result
    }

    override fun collectBlueprintAssets(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile> {
        val sdkData = sdk.aospSdkData ?: return emptySet()
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return emptySet()

        return collectBlueprintSources(sdkData, blueprintFile, blueprint) {
            (this as? BlueprintWithAssets)?.assets ?: emptyList()
        }.mapNotNull { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }.toSet()
    }

    override fun collectBlueprintAidls(blueprint: Blueprint, sdk: Sdk): Set<VirtualFile> {
        val sdkData = sdk.aospSdkData ?: return emptySet()
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return emptySet()

        return collectBlueprintSources(sdkData, blueprintFile, blueprint) {
            (this as? BlueprintWithAidls)?.aidls ?: emptyList()
        }.mapNotNull { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }.toSet()
    }

    override fun getBlueprintManifest(blueprint: Blueprint, sdk: Sdk): VirtualFile? {
        val sdkData = sdk.aospSdkData ?: return null
        val path = AospSdkHelper.getCachePath(blueprint, sdk) ?: return null
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return null

        return when (blueprint) {
            is BlueprintWithManifest -> File(blueprintFile.parent, blueprint.manifest)
            is BlueprintWithDynamicManifest -> blueprint.getManifest(path)
            else -> null
        }?.let { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }
    }

    override fun getBlueprintR(blueprint: Blueprint, sdk: Sdk): VirtualFile? {
        val path = AospSdkHelper.getCachePath(blueprint, sdk) ?: return null

        return when (blueprint) {
            is BlueprintWithResources -> blueprint.getR(path)
            is BlueprintWithDynamicResources -> blueprint.getR(path)
            else -> null
        }?.let { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }
    }

    override fun getBlueprintResApk(blueprint: Blueprint, sdk: Sdk): VirtualFile? {
        val path = AospSdkHelper.getCachePath(blueprint, sdk) ?: return null

        return when (blueprint) {
            is BlueprintWithResources -> blueprint.getResApk(path)
            is BlueprintWithDynamicResources -> blueprint.getResApk(path)
            else -> null
        }?.let { file ->
            VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
        }
    }

    override fun collectBlueprintSources(blueprint: Blueprint, sdk: Sdk, includeGenerated: Boolean): Set<File> {
        val sdkData = sdk.aospSdkData ?: return emptySet()
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return emptySet()

        val sources = mutableSetOf<File>()
        collectBlueprintSources(sdkData, blueprintFile, blueprint) {
            val result = mutableListOf<SourceSet>()

            (this as? BlueprintWithSources)?.sources?.apply {
                result.addAll(this)
            }
            if (includeGenerated) {
                val cachePath = AospSdkHelper.getCachePath(blueprint, sdk)
                if (cachePath != null && blueprint is BlueprintWithDynamicSources) {
                    result.add(PathItem(blueprint.getSources(cachePath)))
                }
            }
            result
        }.forEach(sources::add)

        return sources
    }

    override fun collectGeneratedSources(blueprint: Blueprint, sdk: Sdk): Set<File> {
        val sdkData = sdk.aospSdkData ?: return emptySet()
        val blueprintFile = sdkData.getBlueprintFile(blueprint.name)?.let { File(it.path) } ?: return emptySet()

        val sources = mutableSetOf<File>()
        collectBlueprintSources(sdkData, blueprintFile, blueprint) {
            val cachePath = AospSdkHelper.getCachePath(blueprint, sdk)
            if (cachePath != null && blueprint is BlueprintWithDynamicSources) {
                listOf(PathItem(blueprint.getSources(cachePath)))
            } else {
                emptyList()
            }
        }.forEach(sources::add)

        return sources
    }

    override fun collectBlueprintDependencies(blueprint: Blueprint, sdk: Sdk): List<Blueprint> {
        val dependencies = mutableMapOf<String, Blueprint>()
        val toProcess = mutableListOf(blueprint)
        val sdkData = sdk.aospSdkData ?: return emptyList()

        do {
            val item = toProcess.pop()
            if (item is BlueprintWithDependencies) {
                item.dependencies.filterNot(dependencies::containsKey)
                    .mapNotNull { name ->
                        val file = sdkData.getBlueprintFile(name)
                        if (file != null) {
                            BlueprintsTable.get(file)
                        } else {
                            null
                        }
                    }
                    .flatMap { it }
                    .filterNot { dependencies.containsKey(it.name) }
                    .forEach {
                        toProcess.add(it)
                        dependencies[it.name] = it
                    }
            }
        } while (toProcess.isNotEmpty())

        return dependencies.values.toList()
    }

    private fun collectBlueprintSources(data: AospSdkData, blueprintFile: File, blueprint: Blueprint, fetchSource: Blueprint.() -> List<SourceSet>): Set<File> {
        // List of source links and defaults
        val sourceLibs = LinkedList<String>()
        val sources = mutableSetOf<File>()

        getBlueprintSources(blueprintFile, sources, blueprint, fetchSource, sourceLibs)
        while(true) {
            val dependencyName = sourceLibs.pollFirst() ?: break
            val dependencyFile = data.projects[dependencyName]?.let { File(it) } ?: continue
            val blueprints = LocalFileSystem.getInstance().findFileByIoFile(dependencyFile)?.let {
                BlueprintsTable.get(it)
            } ?: continue

            blueprints.firstOrNull { it.name == dependencyName }?.let { dependencyBlueprint ->
                getBlueprintSources(dependencyFile, sources, dependencyBlueprint, fetchSource, sourceLibs)
            }
        }

        return sources
    }

    private fun getBlueprintSources(blueprintFile: File, sources: MutableSet<File>, blueprint: Blueprint, fetchSource:Blueprint.() -> List<SourceSet>, sourceLinks: Deque<String>) {
        val sourcesSet = fetchSource(blueprint)
        val hasSources = if (sourcesSet.isNotEmpty()) {
            sourcesSet.forEach { source ->
                when (source) {
                    is GlobItem -> {
                        if (source.isPattern() || source.isFolder() || source.fileExtension()?.lowercase(Locale.ENGLISH) in LIBRARY_TYPES) {
                            sources.add(source.toFullPath(blueprintFile.parentFile))
                        }
                    }
                    is SourceLink -> sourceLinks.push(source.library)
                    is PathItem -> sources.add(source.path)
                }
            }
            true
        } else {
            false
        }

        if (!hasSources && blueprint is BlueprintWithDefaults) {
            blueprint.defaults.forEach {
                sourceLinks.push(it)
            }
        }
    }
}
