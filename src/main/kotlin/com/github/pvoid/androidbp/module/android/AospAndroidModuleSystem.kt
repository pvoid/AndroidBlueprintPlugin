/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.projectmodel.ExternalLibrary
import com.android.projectmodel.Library
import com.android.projectmodel.RecursiveResourceFolder
import com.android.tools.idea.project.ModuleBasedClassFileFinder
import com.android.tools.idea.projectsystem.*
import com.android.tools.idea.res.MainContentRootSampleDataDirectoryProvider
import com.android.tools.idea.run.AndroidDeviceSpec
import com.android.tools.idea.run.ApkProvider
import com.android.tools.idea.run.ApplicationIdProvider
import com.android.tools.idea.util.toPathString
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.BlueprintsTable
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.AospSdkType
import com.google.common.collect.ImmutableList
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.util.text.nullize
import org.jetbrains.android.dom.manifest.cachedValueFromPrimaryManifest
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidUtils
import java.io.File

private val PACKAGE_NAME = Key.create<CachedValue<String?>>("merged.manifest.package.name")

class AospAndroidModuleSystem(
    val project: Project,
    override val module: Module
) : AndroidModuleSystem,
    ClassFileFinder by ModuleBasedClassFileFinder(module),
    SampleDataDirectoryProvider by MainContentRootSampleDataDirectoryProvider(module) {

    override fun analyzeDependencyCompatibility(dependenciesToAdd: List<GradleCoordinate>): Triple<List<GradleCoordinate>, List<GradleCoordinate>, String> {
        return Triple(emptyList(), dependenciesToAdd, "")
    }

    override fun canGeneratePngFromVectorGraphics(): CapabilityStatus = CapabilityNotSupported()

    override fun canRegisterDependency(type: DependencyType): CapabilityStatus = CapabilityNotSupported()

    override fun getDirectResourceModuleDependents(): List<Module>
        = ModuleManager.getInstance(module.project).getModuleDependentModules(module)

    override fun getManifestOverrides(): ManifestOverrides
        = ManifestOverrides()

    override fun getModuleTemplates(targetDirectory: VirtualFile?): List<NamedModuleTemplate>
        = emptyList()

    override val isMlModelBindingEnabled: Boolean = false

    override fun getPackageName(): String? {
        val facet = AndroidFacet.getInstance(module)!!
        val cachedValue = facet.cachedValueFromPrimaryManifest {
            packageName.nullize(true)
        }
        return facet.putUserDataIfAbsent(PACKAGE_NAME, cachedValue).value
    }

    override fun getRegisteredDependency(coordinate: GradleCoordinate): GradleCoordinate? = null

    override fun getResolveScope(scopeType: ScopeType): GlobalSearchScope
        = module.getModuleWithDependenciesAndLibrariesScope(false)

    override fun getResolvedDependency(coordinate: GradleCoordinate): GradleCoordinate? {
        return null
    }

    override fun getResolvedDependentLibraries(includeExportedTransitiveDeps: Boolean): Collection<Library> {
        val result = mutableListOf<Library>()
        val manager = ProjectRootManager.getInstance(project)
        val sdk = manager.projectSdk ?: return emptyList()

        val orderEnumerator = ModuleRootManager.getInstance(module)
          .orderEntries()
          .librariesOnly()

        if (includeExportedTransitiveDeps) {
          orderEnumerator.recursively().exportedOnly()
        }

        orderEnumerator.forEachLibrary { library ->
            val libraryName = library.name ?: return@forEachLibrary true
            val blueprintFile = AospSdkHelper.getLibraryBlueprint(libraryName, sdk) ?: return@forEachLibrary true
            val blueprints = BlueprintsTable.get(blueprintFile)

            val libraryBlueprint = blueprints.firstOrNull { it.name == libraryName } ?: return@forEachLibrary true

            val res = BlueprintHelper.collectBlueprintResources(libraryBlueprint, sdk)
            if (res.isEmpty()) {
                return@forEachLibrary true
            }

            val manifest = BlueprintHelper.getBlueprintManifest(libraryBlueprint, sdk)
                    ?: return@forEachLibrary true
            val symbolFile = BlueprintHelper.getBlueprintR(libraryBlueprint, sdk)
                    ?: return@forEachLibrary true
            val resApk  = BlueprintHelper.getBlueprintResApk(libraryBlueprint, sdk)
                    ?: return@forEachLibrary true

            result.add(ExternalLibrary(
                    address = libraryName,
                    resFolder =  RecursiveResourceFolder(res.first().toPathString()),
                    manifestFile = manifest.toPathString(),
                    classJars = library.getFiles(OrderRootType.CLASSES).map { it.toPathString() },
                    symbolFile = symbolFile.toPathString(),
                    resApkFile = resApk.toPathString()
            ))

            true
        }

        return ImmutableList.copyOf(result)
    }

    override fun getResourceModuleDependencies(): List<Module>
        = AndroidUtils.getAllAndroidDependencies(module, true).map(AndroidFacet::getModule)

    override fun registerDependency(coordinate: GradleCoordinate) {
        registerDependency(coordinate, DependencyType.IMPLEMENTATION)
    }

    override fun registerDependency(coordinate: GradleCoordinate, type: DependencyType) {
    }

    override val codeShrinker: CodeShrinker? = null

    override val isRClassTransitive: Boolean = true

    override val usesCompose: Boolean = false

    override fun getDynamicFeatureModules(): List<Module> = emptyList()

    override fun getMergedManifestContributors(): MergedManifestContributors = defaultGetMergedManifestContributors()

    override fun getResolvedLibraryDependencies(): Collection<Library> = getResolvedDependentLibraries(includeExportedTransitiveDeps = true)

    override fun getTestArtifactSearchScopes(): TestArtifactSearchScopes? = null

    override fun getApkProvider(
        runConfiguration: RunConfiguration,
        targetDeviceSpec: AndroidDeviceSpec?
    ): ApkProvider? {
        return null // TODO
    }

    override fun getApplicationIdProvider(runConfiguration: RunConfiguration): ApplicationIdProvider {
        return BlueprintIdProvider(module)
    }

    override fun getNotRuntimeConfigurationSpecificApplicationIdProviderForLegacyUse(): ApplicationIdProvider {
        return BlueprintIdProvider(module)
    }

    override val submodules: Collection<Module>
        = emptyList() //getSubmodules(project, null)
}