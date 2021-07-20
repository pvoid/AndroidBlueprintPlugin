/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module

import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.ui.AndroidBpWizardStep
import com.intellij.ide.util.importProject.ModuleDescriptor
import com.intellij.ide.util.importProject.ProjectDescriptor
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory
import com.intellij.ide.util.projectWizard.importSources.*
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.FileReader
import javax.swing.Icon

class AndroidBpProjectStructureDetector : ProjectStructureDetector() {

    override fun detectRoots(
        dir: File,
        children: Array<out File>,
        base: File,
        result: MutableList<DetectedProjectRoot>
    ): DirectoryProcessingResult {
        val blueprintFile = children.asSequence().firstOrNull { it.name == "Android.bp" }
        if (blueprintFile != null) {
            val blueprints = FileReader(blueprintFile).use {
                val factory = BlueprintCupSymbolFactory(blueprintFile.parentFile)
                BlueprintParser(BlueprintLexer(it, factory)).parse()
                factory.blueprints
            }

            if (!blueprints.isNullOrEmpty()) {
                result.add(AndroidBpDetectedSourceRoot(dir, blueprints))
                return DirectoryProcessingResult.SKIP_CHILDREN
            }
        }

        return DirectoryProcessingResult.PROCESS_CHILDREN
    }

    override fun setupProjectStructure(
        roots: MutableCollection<DetectedProjectRoot>,
        projectDescriptor: ProjectDescriptor,
        builder: ProjectFromSourcesBuilder
    ) {
        val existingRoots = EP_NAME.extensions.filter { it !== this }
            .flatMap { builder.getProjectRoots(it) }
            .map { it.directory }
            .toList()

        val modules = mutableListOf<ModuleDescriptor>()

        for (root in roots) {
            if (root !is AndroidBpDetectedSourceRoot) {
                continue
            }

            val content = mutableMapOf<String, MutableList<DetectedSourceRoot>>()

            root.blueprints.flatMap { blueprint ->
                if (blueprint is BlueprintWithSources) {
                    blueprint.sources.mapNotNull { it as? GlobItem }.map {
                        blueprint.name to it.toFullPath(root.directory)
                    }
                } else {
                    emptyList()
                }
            }.toHashSet().filterNot { (_ , sourceRoot) ->
                existingRoots.any { existingRoot ->
                    FileUtil.isAncestor(existingRoot, sourceRoot, false)
                }
            }.map { (name, sourceRoot) ->
                name to JavaModuleSourceRoot(sourceRoot, "JAVA", false)
            }.forEach { (name, sourceRoot) ->
                content.getOrPut(name) {
                    mutableListOf()
                }.add(sourceRoot)
            }

            content.map { (name, sourceRoots) ->
                ModuleDescriptor(root.directory, JavaModuleType.getModuleType(), sourceRoots).also {
                    it.name = name
                }
            }.forEach(modules::add)
        }

        projectDescriptor.modules = modules
    }

    override fun createWizardSteps(
        builder: ProjectFromSourcesBuilder,
        projectDescriptor: ProjectDescriptor,
        stepIcon: Icon
    ): MutableList<ModuleWizardStep> = mutableListOf(
        ProjectWizardStepFactory.getInstance().createProjectJdkStep(builder.context),
        AndroidBpWizardStep(AndroidBpModuleBuilder())
    )
}

class AndroidBpDetectedSourceRoot(
    path: File,
    val blueprints: List<Blueprint>
) : DetectedSourceRoot(path, "") {
    override fun getRootTypeName(): String = "Android blueprint"
}
