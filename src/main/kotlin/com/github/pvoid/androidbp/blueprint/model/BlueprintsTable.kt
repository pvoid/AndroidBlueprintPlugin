/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.android.tools.idea.util.toIoFile
import com.github.pvoid.androidbp.LOG
import com.github.pvoid.androidbp.blueprint.BlueprintLanguage
import com.github.pvoid.androidbp.blueprint.BlueprintLexerAdapter
import com.github.pvoid.androidbp.blueprint.parser.BlueprintParser
import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.AospSdkType
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.impl.file.impl.FileManager
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.util.messages.Topic
import java.io.File
import java.io.IOException

interface BlueprintChangeNotifier {

    fun beforeAction(file: VirtualFile, blueprints: List<Blueprint>)
    fun afterAction(file: VirtualFile, blueprints: List<Blueprint>)

    companion object {
        val CHANGE_TOPIC =
            Topic.create("Blueprint file changed", BlueprintChangeNotifier::class.java)
    }
}

interface BlueprintsTable {

    fun get(blueprintFile: VirtualFile): List<Blueprint>
    fun update(project: Project, blueprintFiles: List<VirtualFile>)
    fun parse(file: VirtualFile): List<Blueprint>

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

    override fun parse(file: VirtualFile): List<Blueprint> {
        var result: List<Blueprint>? = null
        ApplicationManager.getApplication().runReadAction {
            val parser = BlueprintParser()
            LOG.info("Processing file: $file")
            try {
                val text = file.toIoFile().readText()
                val builder = PsiBuilderFactory.getInstance().createBuilder(
                    LanguageParserDefinitions.INSTANCE.forLanguage(BlueprintLanguage.INSTANCE),
                    BlueprintLexerAdapter(),
                    text
                )
                val element = FileElement(BlueprintTypes.DUMNMY, text)
                parser.parse(element.elementType, builder) as? FileElement
                result = builder.treeBuilt.toBlueprints(File(file.parent.path))
            } catch (e: IOException) {
                LOG.error(e)
            }
        }
        return result ?: emptyList()
    }
}