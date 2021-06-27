/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.AospSdkType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.gradle.tooling.model.ProjectModel
import java.io.File
import java.io.FileReader

interface BlueprintChangeNotifier {

    fun beforeAction(file: VirtualFile, blueprints: List<Blueprint>)
    fun afterAction(file: VirtualFile, blueprints: List<Blueprint>)

    companion object {
        val CHANGE_TOPIC =
            Topic.create<BlueprintChangeNotifier>("Blueprint file changed", BlueprintChangeNotifier::class.java)
    }
}

interface BlueprintsTable {

    fun get(blueprintFile: VirtualFile): List<Blueprint>
    fun update(project: Project, blueprintFiles: List<VirtualFile>)

    companion object : BlueprintsTable by BlueprintsTableImpl() {
    }
}

class BlueprintsTableImpl : BlueprintsTable {

    private val mCache = mutableMapOf<String, List<Blueprint>>()

    override fun get(blueprintFile: VirtualFile): List<Blueprint> {
        synchronized(this) {
            mCache[blueprintFile.url]?.let {
                return it
            }
        }

        val blueprint = parse(blueprintFile)

        if (blueprint.isNotEmpty()) {
            synchronized(this) {
                mCache.putIfAbsent(blueprintFile.url, blueprint)
            }
        }

        return blueprint
    }

    override fun update(project: Project, blueprintFiles: List<VirtualFile>) {
        object : Task.Backgroundable(project, "Updating cached blueprints...", false) {
            override fun run(indicator: ProgressIndicator) {
                blueprintFiles.forEach { blueprintFile ->
                    val blueprint = parse(blueprintFile)
                    synchronized(this) {
                        val current = mCache[blueprintFile.url] ?: emptyList()
                        val publisher = project.messageBus.syncPublisher(BlueprintChangeNotifier.CHANGE_TOPIC)

                        publisher.beforeAction(blueprintFile, current)

                        if (blueprint.isNotEmpty()) {
                            mCache[blueprintFile.url] = blueprint
                        } else {
                            mCache.remove(blueprintFile.url)
                        }

                        publisher.afterAction(blueprintFile, current)
                    }

                    val sdk = ProjectRootManager.getInstance(project).projectSdk
                    if (sdk != null && sdk.sdkType == AospSdkType.INSTANCE) {
                        AospSdkHelper.updateAdditionalDataForBlueprint(sdk, blueprintFile, indicator)
                    }
                }
            }
        }.queue()
    }

    private fun parse(blueprintFile: VirtualFile): List<Blueprint> {
        val file = File(blueprintFile.path)
        return try {
            FileReader(file).use {
                val factory = BlueprintCupSymbolFactory(file.parentFile)
                BlueprintParser(BlueprintLexer(it, factory)).parse()
                factory.blueprints
            }
        } catch (e: RuntimeException) {
// TODO:           LOG.error("Blueprint parsing error", e)
            emptyList()
        }
    }
}