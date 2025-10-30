/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.repository.WellKnownMavenArtifactId
import com.android.ide.common.util.PathString
import com.android.manifmerger.ManifestSystemProperty
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.tools.idea.navigator.getSubmodules
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.DependencyScopeType
import com.android.tools.idea.projectsystem.ManifestOverrides
import com.android.tools.idea.projectsystem.NamedModuleTemplate
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.res.AndroidDependenciesCache
import com.android.tools.module.ModuleDependencies
import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.DependenciesScope
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.facet.AndroidFacet

class BlueprintModuleSystem(
    override val module: Module
) : AndroidModuleSystem {

    var blueprints: List<Blueprint> = emptyList()
        private set

    private var dependencies: List<Pair<DependenciesScope, Blueprint>> = emptyList()

    private val classFinder = BlueprintModuleClassFinder(module)

    override val moduleDependencies: ModuleDependencies = BlueprintModuleDependencies(module)

    fun updateBlueprints(blueprints: List<Blueprint>, dependencies: Collection<Pair<DependenciesScope, Blueprint>>) {
        val deps = dependencies.filterNot { (_, blueprint) -> blueprint.packageName().isNullOrEmpty() }
        this.blueprints = blueprints

        synchronized(this) {
            this@BlueprintModuleSystem.dependencies = deps
        }

        deps.forEach { (scope, blueprint) ->
            Log.warn("Android ${ if (scope == DependenciesScope.Static) "static" else "dynamic" } dependency registered - ${blueprint.packageName()}")
        }

        (moduleDependencies as BlueprintModuleDependencies).updateDependencies(blueprints, deps)
        BlueprintClassJarProvider.updateExternalJars(module, deps)
    }

    fun blueprintByPackageName(packageName: String): Blueprint? {
        return blueprints.firstOrNull { it.packageName() == packageName || packageName.startsWith("${it.packageName()}.") }
    }

    @Deprecated("ClassFileFinder needs to be requested in a context of a specific file.")
    override val moduleClassFileFinder: ClassFileFinder = classFinder

    override fun getAndroidLibraryDependencies(scope: DependencyScopeType): Collection<ExternalAndroidLibrary> {
        val rootPath = module.project.guessAospRoot()?.let(SoongTools::getOutputPath)
            ?: return emptyList()
        val deps = synchronized(this) {
            dependencies
        }

        return deps.filter { (_, blueprint) ->
            blueprint.isAndroidProject() || blueprint.isAndroidImport()
        }.map { (_, blueprint) ->
            val record = AndroidDependencyRecord.Builder(blueprint.name)
                .withPackageName(blueprint.packageName())
                .withR(blueprint.R(rootPath))
                .withManifest(blueprint.manifest())

            blueprint.outputJars(rootPath).filter { it.exists() }.foldRight(record) { jar, r ->
                r.withJar(jar)
            }

            blueprint.assets().filter { it.exists() }.foldRight(record) { assets, r ->
                r.withAssets(assets)
            }

            if (blueprint.isAndroidImport()) {
                blueprint.resApk(rootPath)?.takeIf { it.exists() }?.also { record.withResApk(it) }
                blueprint.generatedResources(rootPath).filter { it.exists() }.foldRight(record) { res, r ->
                    r.withGeneratedRes(res)
                }
            } else {
                blueprint.resources().filter { it.exists() }.foldRight(record) { res, r ->
                    r.withRes(res)
                }
            }

            record.build()
        }.mapNotNull {
            if (it.R.isNotEmpty()) {
                LibrariesTools.createAndroidLibrary(it)
            } else {
                null
            }
        }
    }

    override fun getDirectResourceModuleDependents(): List<Module> {
        return ModuleManager.getInstance(module.project).getModuleDependentModules(module)
    }

    override fun getManifestOverrides(): ManifestOverrides {
        val overrides = mutableMapOf<ManifestSystemProperty, String>()
        val version = module.project.guessPlatformVersion()?.toString() ?: "29"

        overrides[ManifestSystemProperty.UsesSdk.TARGET_SDK_VERSION] = version
        overrides[ManifestSystemProperty.UsesSdk.MIN_SDK_VERSION] = version

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

    override fun getResolveScope(scopeType: ScopeType): GlobalSearchScope {
        return GlobalSearchScope.allScope(module.project)
    }

    override val applicationRClassConstantIds: Boolean = false

    override val testRClassConstantIds: Boolean = false

    override fun getResourceModuleDependencies(): List<Module> =
        AndroidDependenciesCache.getAllAndroidDependencies(module, true).map(AndroidFacet::getModule)

    override fun getSampleDataDirectory(): PathString? = null

    override val submodules: Collection<Module>
        get() = getSubmodules(module.project, module)

    override fun hasResolvedDependency(id: WellKnownMavenArtifactId, scope: DependencyScopeType): Boolean = false
}