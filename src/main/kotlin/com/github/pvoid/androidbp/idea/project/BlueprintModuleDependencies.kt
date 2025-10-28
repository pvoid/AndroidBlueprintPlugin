/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.repository.GoogleMavenArtifactId
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.rendering.PsiClassViewClass
import com.android.tools.module.ModuleDependencies
import com.android.tools.module.ViewClass
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

class BlueprintModuleDependencies(
    private val module: Module
) : ModuleDependencies {
    private var packages = emptyList<String>()

    override fun dependsOn(artifactId: GoogleMavenArtifactId): Boolean = false

    override fun findViewClass(fqcn: String): ViewClass? {
        val facade = JavaPsiFacade.getInstance(module.project)
//        LOG.warn("BlueprintModuleDependencies: looking for '$fqcn'")
        return facade.findClass(fqcn, module.getModuleWithDependenciesAndLibrariesScope(false))?.let { PsiClassViewClass(it) }
    }

    override fun getResourcePackageNames(includeExternalLibraries: Boolean): List<String> {
        val result = if (includeExternalLibraries) {
            packages.toMutableSet()
        } else {
            HashSet<String>()
        }

        (module.getModuleSystem() as? BlueprintModuleSystem)?.blueprints?.mapNotNull {
            it.packageName()
        }?.toCollection(result)

        return result.toList()
    }

    override fun dependsOnAndroidx(): Boolean = false

    fun updateDependencies(blueprints: Collection<BlueprintExternalLibrary>) {
        packages = blueprints.filter { it.hasResources }.mapNotNull { it.packageName }.toList()
    }
}
