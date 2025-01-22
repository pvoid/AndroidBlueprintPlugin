/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.projectsystem.*
import com.android.tools.idea.util.toVirtualFile
import com.github.pvoid.androidbp.blueprint.BlueprintType
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.facet.AndroidFacet

private val EmptyProvider = object : NamedIdeaSourceProvider {
    override val name: String = "main"
    override val scopeType: ScopeType = ScopeType.MAIN
    override val aidlDirectories: Iterable<VirtualFile> = emptyList()
    override val aidlDirectoryUrls: Iterable<String> = emptyList()
    override val assetsDirectories: Iterable<VirtualFile> = emptyList()
    override val assetsDirectoryUrls: Iterable<String> = emptyList()
    override val custom: Map<String, IdeaSourceProvider.Custom> = emptyMap()
    override val javaDirectories: Iterable<VirtualFile> = emptyList()
    override val javaDirectoryUrls: Iterable<String> = emptyList()
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
    override val shadersDirectories: Iterable<VirtualFile> = emptyList()
    override val shadersDirectoryUrls: Iterable<String> = emptyList()
    override val baselineProfileDirectories: Iterable<VirtualFile> = emptyList()
    override val baselineProfileDirectoryUrls: Iterable<String> = emptyList()
}

class BlueprintSourceProvidersFactory : SourceProvidersFactory {

    private val emptyProvider = SourceProvidersImpl(
        mainIdeaSourceProvider = EmptyProvider,
        currentSourceProviders = emptyList(),
        currentHostTestSourceProviders = emptyMap(),
        currentDeviceTestSourceProviders = emptyMap(),
        currentTestFixturesSourceProviders = emptyList(),
        currentAndSomeFrequentlyUsedInactiveSourceProviders = emptyList(),
        mainAndFlavorSourceProviders = emptyList(),
        generatedSources = createMergedSourceProvider(ScopeType.MAIN, emptyList()),
        generatedHostTestSources = emptyMap(),
        generatedDeviceTestSources = emptyMap(),
        generatedTestFixturesSources = createMergedSourceProvider(ScopeType.TEST_FIXTURES, emptyList()),
        allVariantAllArtifactsSourceProviders = emptyList()
    )

    private val cachedProviders = mutableMapOf<AndroidFacet, SourceProviders>()

    override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
        val moduleSystem = (facet.getModuleSystem() as? BlueprintModuleSystem)

        val (name, manifest) = moduleSystem?.blueprints?.filter {
            it.type == BlueprintType.AndroidApp || it.type == BlueprintType.AndroidLibrary
        }?.mapNotNull { blueprint ->
            blueprint.manifest()?.let { blueprint.name to it }
        }?.firstNotNullOfOrNull { (name, manifest) ->
            manifest.toVirtualFile()?.let { name to it }
        } ?: return emptyProvider

        val cached = cachedProviders.getOrPut(facet) {
            val provider = BlueprintSourceProvider(name, ScopeType.MAIN, moduleSystem, manifest)
            BlueprintSourceProviders(provider)
        }
        return cached
    }
}

private class BlueprintSourceProviders(
    provider: NamedIdeaSourceProvider
) : SourceProviders {
    override val sources: IdeaSourceProvider = provider

    @Deprecated("Do not use. This is unlikely to be what anybody needs.")
    override val mainAndFlavorSourceProviders: List<NamedIdeaSourceProvider> = listOf(provider)

    override val mainIdeaSourceProvider: NamedIdeaSourceProvider = provider

    override val currentAndSomeFrequentlyUsedInactiveSourceProviders: List<NamedIdeaSourceProvider> = emptyList()

    override val currentSourceProviders: List<NamedIdeaSourceProvider> = listOf(provider)

    override val currentTestFixturesSourceProviders: List<NamedIdeaSourceProvider> = emptyList()

    override val generatedSources: IdeaSourceProvider = EmptyProvider

    override val generatedTestFixturesSources: IdeaSourceProvider = EmptyProvider

    override val testFixturesSources: IdeaSourceProvider = EmptyProvider

    override val currentDeviceTestSourceProviders: Map<TestComponentType.DeviceTest, List<NamedIdeaSourceProvider>> = emptyMap()

    override val currentHostTestSourceProviders: Map<TestComponentType.HostTest, List<NamedIdeaSourceProvider>> = emptyMap()

    override val deviceTestSources: Map<TestComponentType.DeviceTest, IdeaSourceProvider> = emptyMap()

    override val generatedDeviceTestSources: Map<TestComponentType.DeviceTest, IdeaSourceProvider> = emptyMap()

    override val generatedHostTestSources: Map<TestComponentType.HostTest, IdeaSourceProvider> = emptyMap()

    override val hostTestSources: Map<TestComponentType.HostTest, IdeaSourceProvider> = emptyMap()

    override val allVariantAllArtifactsSourceProviders: List<NamedIdeaSourceProvider> = listOf(provider)
}
