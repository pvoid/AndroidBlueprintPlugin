/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.repository.GoogleMavenArtifactId
import com.android.tools.idea.rendering.PsiClassViewClass
import com.android.tools.module.ModuleDependencies
import com.android.tools.module.ViewClass
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.DependenciesScope
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade

class BlueprintModuleDependencies(
    private val module: Module
) : ModuleDependencies {
    private var packages = emptyList<Pair<DependenciesScope, String>>()

    private val classFinder = BlueprintModuleClassFinder(module)

    override fun dependsOn(artifactId: GoogleMavenArtifactId): Boolean = false

    override fun findViewClass(fqcn: String): ViewClass? {
        val facade = JavaPsiFacade.getInstance(module.project)
        return facade.findClass(fqcn, module.getModuleWithDependenciesAndLibrariesScope(false))?.let { PsiClassViewClass(it) }
    }

    override fun getResourcePackageNames(includeExternalLibraries: Boolean): List<String> {
        val result = mutableSetOf<String>()
        packages.mapNotNull {  (scope, packageName) ->
            if (scope == DependenciesScope.Static || includeExternalLibraries) {
                packageName
            } else {
                null
            }
        }.toCollection(result)
        return result.toList()
    }

    override fun dependsOnAndroidx(): Boolean = false

    fun updateDependencies(blueprints: Collection<Blueprint>, libraries: Collection<Pair<DependenciesScope, Blueprint>>) {
        packages = libraries.mapNotNull { (scope, blueprint) ->
            blueprint.packageName()?.let {
                scope to it
            }
        }.plus(
            blueprints.mapNotNull { blueprint ->
                blueprint.packageName()?.let { DependenciesScope.Static to it }
            }
        ).toList()
    }
}
