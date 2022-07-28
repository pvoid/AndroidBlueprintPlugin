/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.SdkConstants
import com.android.ide.common.repository.GradleCoordinate
import com.android.projectmodel.*
import com.android.tools.idea.navigator.getSubmodules
import com.android.tools.idea.projectsystem.*
import com.android.tools.idea.res.MainContentRootSampleDataDirectoryProvider
import com.android.tools.idea.run.ApplicationIdProvider
import com.github.pvoid.androidbp.blueprint.model.BlueprintWithArtifacts
import com.github.pvoid.androidbp.blueprint.model.BlueprintsTable
import com.github.pvoid.androidbp.module.AospProjectHelper
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.toJarFileUrl
import com.google.common.collect.ImmutableList
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidUtils
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class AospAndroidModuleSystem(
    val project: Project,
    override val module: Module,
    blueprintsProvider: BlueprintsProvider
) : AndroidModuleSystem,
    ClassFileFinder,
    SampleDataDirectoryProvider by MainContentRootSampleDataDirectoryProvider(module) {

    private val mIdProvider = BlueprintIdProvider(module, blueprintsProvider)

    override val moduleClassFileFinder: ClassFileFinder = this

    override fun analyzeDependencyCompatibility(dependenciesToAdd: List<GradleCoordinate>): Triple<List<GradleCoordinate>, List<GradleCoordinate>, String> {
        return Triple(emptyList(), emptyList(), "")
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

    override fun getPackageName(): String = mIdProvider.packageName

    override fun getRegisteredDependency(coordinate: GradleCoordinate): GradleCoordinate? = null

    override fun getResolveScope(scopeType: ScopeType): GlobalSearchScope
        = module.getModuleWithDependenciesAndLibrariesScope(false)

    override fun getResolvedDependency(coordinate: GradleCoordinate): GradleCoordinate? = null

    override fun getResourceModuleDependencies(): List<Module>
        = AndroidUtils.getAndroidLibraryDependencies(module).map(AndroidFacet::getModule)

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

    override fun getTestArtifactSearchScopes(): TestArtifactSearchScopes? = null

    override val applicationRClassConstantIds: Boolean = false

    override val testRClassConstantIds: Boolean = false

    override val submodules: Collection<Module>
        = getSubmodules(project, null)

    override fun findClassFile(fqcn: String): VirtualFile? {
        return findClassFileInModule(fqcn) ?: findClassInLibraries(fqcn)
    }

    override fun getAndroidLibraryDependencies(scope: DependencyScopeType): Collection<ExternalAndroidLibrary> {
        val result = mutableListOf<ExternalAndroidLibrary>()
        val manager = ProjectRootManager.getInstance(project)
        val sdk = manager.projectSdk ?: return emptyList()

        val orderEnumerator = ModuleRootManager.getInstance(module)
            .orderEntries()
            .librariesOnly()

        orderEnumerator.forEachLibrary { library ->
            val libraryName = library.name ?: return@forEachLibrary true
            val blueprintFile = AospSdkHelper.getLibraryBlueprint(libraryName, sdk) ?: return@forEachLibrary true
            val blueprints = BlueprintsTable.get(blueprintFile)

            val libraryBlueprint = blueprints.firstOrNull { it.name == libraryName } ?: return@forEachLibrary true
            result.add(AospDependency(project, libraryBlueprint))

            true
        }

        return ImmutableList.copyOf(result)
    }

    override fun getDependencyPath(coordinate: GradleCoordinate): Path? = null

    override fun getResolvedDependency(coordinate: GradleCoordinate, scope: DependencyScopeType): GradleCoordinate? = null

    override val isViewBindingEnabled: Boolean = false

    override fun getAndroidLibraryDependencies(): Collection<ExternalAndroidLibrary> {
        return getAndroidLibraryDependencies(DependencyScopeType.MAIN)
    }

    override fun getClassFileFinderForSourceFile(sourceFile: VirtualFile?): ClassFileFinder {
        return moduleClassFileFinder
    }

    override fun getManifestPlaceholders(): Map<String, String> {
        return getManifestOverrides().placeholders // TODO: Should avoid using getManifestOverrides
    }

    override fun getTestPackageName(): String? = null

    private fun findClassFileInModule(fqcn: String): VirtualFile? {
        val blueprintFile = AospProjectHelper.blueprintFileForProject(project) ?: return null
        val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return null

        return BlueprintsTable.get(blueprintFile).asSequence()
            .filterIsInstance(BlueprintWithArtifacts::class.java).flatMap { blueprint ->
                val basePath = AospSdkHelper.getCachePath(blueprint, sdk)
                listOf(File(basePath, "android_common/javac/classes"), File(basePath, "android_common/kotlinc/classes"))
            }.mapNotNull {
                it.toVirtualFile()
            }.mapNotNull {
                findClassFileInOutputRoot(it, fqcn)
            }.firstOrNull()
    }

    private fun findClassInLibraries(fqcn: String): VirtualFile? {
        return ModuleRootManager.getInstance(module).orderEntries.asSequence().filterIsInstance(LibraryOrderEntry::class.java).flatMap {
            it.getFiles(OrderRootType.CLASSES).asSequence()
        }.mapNotNull {
            findClassFileInOutputRoot(it, fqcn)
        }.firstOrNull()
    }

    private fun findClassFileInOutputRoot(outputRoot: VirtualFile, fqcn: String): VirtualFile? {
        if (!outputRoot.exists()) return null

        val pathSegments = fqcn.split(".").toTypedArray()
        pathSegments[pathSegments.size - 1] += SdkConstants.DOT_CLASS
        val outputBase = (JarFileSystem.getInstance().getJarRootForLocalFile(outputRoot) ?: outputRoot)

        val classFile = VfsUtil.findRelativeFile(outputBase, *pathSegments)
            ?: VfsUtil.findFile(Paths.get(outputBase.path, *pathSegments), true)

        return if (classFile != null && classFile.exists()) classFile else null
    }
}
