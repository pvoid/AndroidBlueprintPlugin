/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.idea.navigator.getSubmodules
import com.android.tools.idea.project.DefaultBuildManager
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.LightResourceClassService
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.SourceProvidersFactory
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.res.AndroidInnerClassFinder
import com.android.tools.idea.res.AndroidManifestClassPsiElementFinder
import com.android.tools.idea.res.AndroidResourceClassPsiElementFinder
import com.android.tools.idea.res.ProjectLightResourceClassService
import com.github.pvoid.androidbp.idea.project.sync.BlueprintSyncManager
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.AndroidRootUtil
import java.io.File
import java.nio.file.Path

class BlueprintProjectSystem(
    private val project: Project,
    private val aospRoot: File
) : AndroidProjectSystem {
    override fun allowsFileCreation(): Boolean = false

    private val moduleSystems = mutableMapOf<Module, BlueprintModuleSystem>()

    private val sourceProvidersFactory by lazy {
        BlueprintSourceProvidersFactory()
    }

    private val syncManager: BlueprintSyncManager by lazy {
        BlueprintSyncManager(project, aospRoot)
    }

    override fun getAndroidFacetsWithPackageName(project: Project, packageName: String): Collection<AndroidFacet> {
        val androidFacets = ProjectFacetManager.getInstance(project).getFacets(AndroidFacet.ID)
        return androidFacets.filter {
            val blueprint = (it.getModuleSystem() as? BlueprintModuleSystem)?.blueprintByPackageName(packageName)
            blueprint != null && it.properties.CUSTOM_MANIFEST_PACKAGE == packageName
        }
    }

    override fun getBuildManager(): ProjectSystemBuildManager = DefaultBuildManager

    override fun getClassJarProvider(): ClassJarProvider {
        return object: ClassJarProvider {
            override fun getModuleExternalLibraries(module: Module): List<File> {
                val result = mutableListOf<File>()
                moduleSystems[module]?.dependenciesJars()?.foldRight(result) { jar, r ->
                    r.add(jar)
                    r
                }

                AndroidRootUtil.getExternalLibraries(module)
                    .filterNotNull()
                    .map(VfsUtilCore::virtualToIoFile)
                    .foldRight(result) { jar, r ->
                        r.add(jar)
                        r
                    }

                return result
            }

            override fun isClassFileOutOfDate(module: Module, fqcn: String, classFile: VirtualFile): Boolean {
                return false
            }
        }
    }

    override fun getDefaultApkFile(): VirtualFile? = null

    override fun getLightResourceClassService(): LightResourceClassService = ProjectLightResourceClassService.getInstance(project)

    override fun getModuleSystem(module: Module): AndroidModuleSystem {
        return moduleSystems.getOrCreate(module) {
            BlueprintModuleSystem(module)
        }
    }

    override fun getPathToAapt(): Path = File(aospRoot, "prebuilts/sdk/tools/linux/bin/aapt").toPath()

    override fun getPsiElementFinders(): Collection<PsiElementFinder> {
        return listOf(
            AndroidInnerClassFinder.INSTANCE,
            AndroidManifestClassPsiElementFinder.getInstance(project),
            AndroidResourceClassPsiElementFinder(getLightResourceClassService())
        )
    }

    override fun getSourceProvidersFactory(): SourceProvidersFactory = sourceProvidersFactory

    override fun getSyncManager(): ProjectSystemSyncManager {
        return syncManager
    }

    override val submodules: Collection<Module> = getSubmodules(project, null)
}
