/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.repository.GradleCoordinate
import com.android.ide.common.util.PathString
import com.android.manifmerger.ManifestSystemProperty
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.tools.idea.navigator.getSubmodules
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.CapabilityNotSupported
import com.android.tools.idea.projectsystem.CapabilityStatus
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.DependencyScopeType
import com.android.tools.idea.projectsystem.DependencyType
import com.android.tools.idea.projectsystem.ManifestOverrides
import com.android.tools.idea.projectsystem.NamedModuleTemplate
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.res.AndroidDependenciesCache
import com.android.tools.module.ModuleDependencies
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidUtils
import java.nio.file.Path

class BlueprintModuleSystem(
    override val module: Module
) : AndroidModuleSystem {

    var blueprints: List<Blueprint> = emptyList()
        private set

    private var dependencies: List<BlueprintExternalLibrary> = emptyList()

    private val moduleDependencies_ = BlueprintModuleDependencies(module)

    fun updateBlueprints(blueprints: List<Blueprint>) {
        this.blueprints = blueprints
    }

    fun updateAndroidDependencies(blueprints: Collection<AndroidDependencyRecord>) {
        synchronized(this) {
            dependencies = blueprints.filter {
                it.isValid()
            }.mapNotNull {
                LibrariesTools.createAndroidLibrary(module.project, it)
            }
        }
        moduleDependencies_.updateDependencies(dependencies)
    }

    fun dependenciesJars() = dependencies.flatMap { it.jars }.toList()

    fun blueprintByPackageName(packageName: String): Blueprint? {
        return blueprints.firstOrNull { it.packageName() == packageName || packageName.startsWith("${it.packageName()}.") }
    }

    override val moduleClassFileFinder: ClassFileFinder = BlueprintModuleClassFinder(module)

    override fun analyzeDependencyCompatibility(dependenciesToAdd: List<GradleCoordinate>): Triple<List<GradleCoordinate>, List<GradleCoordinate>, String> {
        return Triple(emptyList(), dependenciesToAdd, "")
    }

    override fun canGeneratePngFromVectorGraphics(): CapabilityStatus = CapabilityNotSupported()

    override fun getAndroidLibraryDependencies(scope: DependencyScopeType): Collection<ExternalAndroidLibrary> {
        return synchronized(this) {
            dependencies
        }
    }

    override fun getDirectResourceModuleDependents(): List<Module> {
        return ModuleManager.getInstance(module.project).getModuleDependentModules(module)
    }

    override fun getManifestOverrides(): ManifestOverrides {
        val overrides = mutableMapOf<ManifestSystemProperty, String>()

        overrides[ManifestSystemProperty.UsesSdk.TARGET_SDK_VERSION] = "29" // TODO: Use the real platform version
        overrides[ManifestSystemProperty.UsesSdk.MIN_SDK_VERSION] = "29"

        return ManifestOverrides(
            directOverrides = overrides
        )
    }

    override fun getModuleTemplates(targetDirectory: VirtualFile?): List<NamedModuleTemplate> = emptyList()

    override fun getOrCreateSampleDataDirectory(): PathString? = null

    override fun getPackageName(): String? {
        return blueprints.firstNotNullOfOrNull {
            it.packageName()
        }
    }

    override fun getRegisteredDependency(coordinate: GradleCoordinate): GradleCoordinate? = null

    override fun getResolveScope(scopeType: ScopeType): GlobalSearchScope {
        return GlobalSearchScope.allScope(module.project)
    }

    override val applicationRClassConstantIds: Boolean = false

    override val testRClassConstantIds: Boolean = false

    override fun getResolvedDependency(coordinate: GradleCoordinate, scope: DependencyScopeType): GradleCoordinate? = null

    override fun getResourceModuleDependencies(): List<Module> =
        AndroidDependenciesCache.getAllAndroidDependencies(module, true).map(AndroidFacet::getModule)

    override fun getSampleDataDirectory(): PathString? = null

    override fun canRegisterDependency(type: DependencyType): CapabilityStatus = CapabilityNotSupported()

    override fun registerDependency(coordinate: GradleCoordinate) {
        registerDependency(coordinate, DependencyType.IMPLEMENTATION)
    }

    override fun registerDependency(coordinate: GradleCoordinate, type: DependencyType) {
    }

    override val submodules: Collection<Module>
        get() = getSubmodules(module.project, module)

    override val moduleDependencies: ModuleDependencies = moduleDependencies_
}