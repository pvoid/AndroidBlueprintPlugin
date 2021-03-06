/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.idea.projectsystem.IdeaSourceProvider
import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.projectsystem.SourceProviders
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.Blueprint
import com.github.pvoid.androidbp.blueprint.model.BlueprintWithManifest
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

class BlueprintSourceProviders(
    private val projectPath: String?,
    private val sdk: Sdk?,
    private val blueprints: List<Blueprint>
) : SourceProviders {

    init {
        if (blueprints.isEmpty()) {
            throw AssertionError("Empty blueprints list")
        }
    }

    private val mAllSourceProviders: List<NamedIdeaSourceProvider>
        get() = if (sdk != null && projectPath != null) {
            blueprints.map { BlueprintSourceProvider(projectPath, sdk, it) }
        } else {
            emptyList()
        }

    override val currentAndSomeFrequentlyUsedInactiveSourceProviders: List<NamedIdeaSourceProvider>
        get() = emptyList() // NOTE: Can be useful

    override val androidTestSources: IdeaSourceProvider
        get() = EmptySourceProvider()

    override val currentAndroidTestSourceProviders: List<NamedIdeaSourceProvider>
        get() = emptyList()

    override val currentSourceProviders: List<NamedIdeaSourceProvider>
        get() = mAllSourceProviders
    override val currentTestFixturesSourceProviders: List<NamedIdeaSourceProvider>
        get() = emptyList()

    override val currentUnitTestSourceProviders: List<NamedIdeaSourceProvider>
        get() = emptyList()
    override val generatedAndroidTestSources: IdeaSourceProvider
        get() = EmptySourceProvider()
    override val generatedSources: IdeaSourceProvider
        get() = if (sdk != null) {
            BlueprintGeneratedSourceProvider(sdk, blueprints)
        } else {
            EmptySourceProvider()
        }
    override val generatedTestFixturesSources: IdeaSourceProvider
        get() = EmptySourceProvider()
    override val generatedUnitTestSources: IdeaSourceProvider
        get() = EmptySourceProvider()

    @Deprecated("Do not use. This is unlikely to be what anybody needs.")
    override val mainAndFlavorSourceProviders: List<NamedIdeaSourceProvider>
        get() = mAllSourceProviders

    override val mainIdeaSourceProvider: NamedIdeaSourceProvider
        get() = mAllSourceProviders.firstOrNull() ?: EmptySourceProvider()

    override val sources: IdeaSourceProvider
        get() = mAllSourceProviders.firstOrNull() ?: EmptySourceProvider()
    override val testFixturesSources: IdeaSourceProvider
        get() = EmptySourceProvider()

    override val unitTestSources: IdeaSourceProvider
        get() = EmptySourceProvider()
}

private class BlueprintSourceProvider(projectPath: String, sdk: Sdk, blueprint: Blueprint) : NamedIdeaSourceProvider {
    override val name: String = blueprint.name
    override val aidlDirectories: Collection<VirtualFile> = BlueprintHelper.collectBlueprintAidls(blueprint, sdk)
    override val aidlDirectoryUrls: Collection<String> = aidlDirectories.map { it.url }
    override val assetsDirectories: Collection<VirtualFile> = BlueprintHelper.collectBlueprintAssets(blueprint, sdk)
    override val assetsDirectoryUrls: Collection<String> = assetsDirectories.map { it.url }
    override val javaDirectories: Collection<VirtualFile> =
        BlueprintHelper.collectBlueprintSources(blueprint, sdk, false).mapNotNull {
            VirtualFileManager.getInstance().findFileByUrl(it.toFileSystemUrl())
        }
    override val javaDirectoryUrls: Collection<String> = javaDirectories.map { it.url }
    override val kotlinDirectories: Iterable<VirtualFile> = emptyList()
    override val kotlinDirectoryUrls: Iterable<String> = emptyList()
    override val jniLibsDirectories: Collection<VirtualFile> = emptyList() // TODO: Add an actual paths
    override val jniLibsDirectoryUrls: Collection<String> = emptyList() // TODO: Add an actual paths
    override val manifestDirectories: Collection<VirtualFile> = emptyList()
    override val manifestDirectoryUrls: Collection<String> = emptyList()
    override val manifestFiles: Collection<VirtualFile> = (blueprint as? BlueprintWithManifest)?.manifest?.let {
        File(projectPath, it)
    }?.let {
        VirtualFileManager.getInstance().findFileByUrl(it.toFileSystemUrl())
    }?.let {
        listOf(it)
    } ?: emptyList()

    override val manifestFileUrls: Collection<String> = manifestFiles.map { it.url }
    override val renderscriptDirectories: Collection<VirtualFile> = emptyList() // TODO: Add an actual paths
    override val renderscriptDirectoryUrls: Collection<String> = emptyList() // TODO: Add an actual paths
    override val resDirectories: Collection<VirtualFile> = BlueprintHelper.collectBlueprintResources(blueprint, sdk)
    override val resDirectoryUrls: Collection<String> = resDirectories.map { it.url }
    override val resourcesDirectories: Collection<VirtualFile> = emptyList()
    override val resourcesDirectoryUrls: Collection<String> = emptyList()
    override val shadersDirectories: Collection<VirtualFile> = emptyList() // TODO: Add an actual paths
    override val shadersDirectoryUrls: Collection<String> = emptyList() // TODO: Add an actual paths
    override val mlModelsDirectories: Collection<VirtualFile> = emptyList()
    override val mlModelsDirectoryUrls: Collection<String> = emptyList()
    override val custom: Map<String, IdeaSourceProvider.Custom> = emptyMap() // TODO: Add an actual paths
    override val scopeType: ScopeType = ScopeType.MAIN
}

private class BlueprintGeneratedSourceProvider(sdk: Sdk, blueprints: List<Blueprint>) : NamedIdeaSourceProvider {
    override val name: String = blueprints.firstOrNull()?.name ?: ""
    override val aidlDirectories: Iterable<VirtualFile> = emptyList()
    override val aidlDirectoryUrls: Iterable<String> = emptyList()
    override val assetsDirectories: Iterable<VirtualFile> = emptyList()
    override val assetsDirectoryUrls: Iterable<String> = emptyList()
    override val custom: Map<String, IdeaSourceProvider.Custom> = emptyMap()
    override val javaDirectories: Iterable<VirtualFile> =
        blueprints.flatMap { BlueprintHelper.collectGeneratedSources(it, sdk) }.mapNotNull {
            VirtualFileManager.getInstance().findFileByUrl(it.toFileSystemUrl())
        }
    override val javaDirectoryUrls: Iterable<String> = javaDirectories.map { it.url }
    override val jniLibsDirectories: Iterable<VirtualFile> = emptyList()
    override val jniLibsDirectoryUrls: Iterable<String> = emptyList()
    override val kotlinDirectories: Iterable<VirtualFile> = emptyList()
    override val kotlinDirectoryUrls: Iterable<String> = emptyList()
    override val manifestDirectories: Iterable<VirtualFile> = emptyList()
    override val manifestDirectoryUrls: Iterable<String> = emptyList()
    override val manifestFileUrls: Iterable<String> = emptyList()
    override val manifestFiles: Iterable<VirtualFile> = emptyList()
    override val mlModelsDirectories: Iterable<VirtualFile> = emptyList()
    override val mlModelsDirectoryUrls: Iterable<String> = emptyList()
    override val renderscriptDirectories: Iterable<VirtualFile> = emptyList()
    override val renderscriptDirectoryUrls: Iterable<String> = emptyList()
    override val resDirectories: Iterable<VirtualFile> = emptyList()
    override val resDirectoryUrls: Iterable<String> = emptyList()
    override val resourcesDirectories: Iterable<VirtualFile> = emptyList()
    override val resourcesDirectoryUrls: Iterable<String> = emptyList()
    override val scopeType: ScopeType = ScopeType.MAIN
    override val shadersDirectories: Iterable<VirtualFile> = emptyList()
    override val shadersDirectoryUrls: Iterable<String> = emptyList()
}

private class EmptySourceProvider : NamedIdeaSourceProvider {
    override val name: String = ""
    override val aidlDirectories: Collection<VirtualFile> = emptyList()
    override val aidlDirectoryUrls: Collection<String> = emptyList()
    override val assetsDirectories: Collection<VirtualFile> = emptyList()
    override val assetsDirectoryUrls: Collection<String> = emptyList()
    override val custom: Map<String, IdeaSourceProvider.Custom> = emptyMap()
    override val javaDirectories: Collection<VirtualFile> = emptyList()
    override val javaDirectoryUrls: Collection<String> = emptyList()
    override val kotlinDirectories: Iterable<VirtualFile> = emptyList()
    override val kotlinDirectoryUrls: Iterable<String> = emptyList()
    override val jniLibsDirectories: Collection<VirtualFile> = emptyList()
    override val jniLibsDirectoryUrls: Collection<String> = emptyList()
    override val manifestDirectories: Collection<VirtualFile> = emptyList()
    override val manifestDirectoryUrls: Collection<String> = emptyList()
    override val manifestFileUrls: Collection<String> = emptyList()
    override val manifestFiles: Collection<VirtualFile> = emptyList()
    override val renderscriptDirectories: Collection<VirtualFile> = emptyList()
    override val renderscriptDirectoryUrls: Collection<String> = emptyList()
    override val resDirectories: Collection<VirtualFile> = emptyList()
    override val resDirectoryUrls: Collection<String> = emptyList()
    override val resourcesDirectories: Collection<VirtualFile> = emptyList()
    override val resourcesDirectoryUrls: Collection<String> = emptyList()
    override val shadersDirectories: Collection<VirtualFile> = emptyList()
    override val shadersDirectoryUrls: Collection<String> = emptyList()
    override val mlModelsDirectories: Collection<VirtualFile> = emptyList()
    override val mlModelsDirectoryUrls: Collection<String> = emptyList()
    override val scopeType: ScopeType = ScopeType.MAIN
}