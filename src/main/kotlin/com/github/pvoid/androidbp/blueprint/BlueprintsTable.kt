/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.blueprint.parser.BlueprintParser
import com.github.pvoid.androidbp.blueprint.parser.toBlueprints
import com.github.pvoid.androidbp.idea.LOG
import com.github.pvoid.androidbp.idea.project.guessAospRoot
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.impl.source.tree.FileElement
import java.io.File
import java.io.IOException

private val TABLES = mutableMapOf<Project, BlueprintsTable>()

private val DUMNMY = BlueprintElementType("DUMMY")

private class BlueprintsTableCacheItem(
    val timestamp: Long,
    val blueprints: List<Blueprint>
)

class BlueprintsTable(project: Project) {

    private val aospRoot: File? = project.guessAospRoot()

    private var files = emptyList<File>()

    private var blueprints = emptyMap<String, File>()

    private val cache = mutableMapOf<File, BlueprintsTableCacheItem>()

    @Synchronized
    fun update(files: List<File>, blueprints: Map<String, File>) {
        files.forEach {
            VirtualFileManager.getInstance().findFileByNioPath(it.toPath())
        }
        this.files = files
        this.blueprints = blueprints
    }

    fun availableBlueprints(): Collection<String> = blueprints.keys

    operator fun get(name: String): Blueprint? {
        val fixedName = fixUpName(name)
        val file =  synchronized(this) {
            blueprints[fixedName]
        } ?: return null

        return parse(file).firstOrNull { blueprint ->
            blueprint.name == fixedName
        }
    }

    fun parse(file: File): List<Blueprint> {
        if (!file.exists()) {
            return emptyList()
        }

        val timestamp = file.lastModified()
        synchronized(this) {
            val record = cache[file]
            if (record?.timestamp == timestamp) {
                return record.blueprints
            }
        }

        if (aospRoot == null) {
            return emptyList()
        }

        val blueprints = parse(aospRoot, file, mutableListOf())
        if (blueprints.isNotEmpty()) {
            synchronized(this) {
                cache[file] = BlueprintsTableCacheItem(timestamp, blueprints)
            }
        }
        return blueprints
    }

    private fun fixUpName(name: String): String {
        // Convert stubs to real link
        if (name.endsWith(".stubs") && !blueprints.containsKey(name)) {
            return name.dropLast(6)
        }

        // Convert java aidls or hidl to a real link
        // TODO: Add -java-constants support
        if (name.endsWith("-java") && !blueprints.containsKey(name)) {
            var drop = 5
            var version: String? = null
            // Check if it's a link to HIDL
            val pos = name.asSequence().take(name.length - drop).indexOfLast { it == '-' }
            if (pos != -1 && name[pos + 1] == 'V') {
                version = name.substring(pos + 2, name.length - drop)
                if (version.all { it.isDigit() || it == '.' }) {
                    drop = name.length - pos
                } else {
                    version = null
                }
            }

            return name.dropLast(drop).let { fixedName ->
                version?.let {
                    "$fixedName@$version"
                } ?: fixedName
            }
        }

        return name
    }

    companion object {
        fun getInstance(project: Project): BlueprintsTable {
            return TABLES.getOrPut(project) {
                BlueprintsTable(project)
            }
        }

        fun parse(aospRoot: File, file: File, extra: MutableList<File>?): List<Blueprint> {
            LOG.info("Processing file: $file")

            val parser = BlueprintParser()
            val text = try {
                file.readText()
            } catch (e: IOException) {
                LOG.error(e)
                return emptyList()
            }
            val builder = createPsiBuilder(text)

            val element = FileElement(DUMNMY, text)
            parser.parse(element.elementType, builder) as? FileElement
            return builder.treeBuilt.toBlueprints(aospRoot, File(file.parentFile.path), extra)
        }

        private fun createPsiBuilder(text: String): PsiBuilder {
            return ApplicationManager.getApplication().runReadAction(
                Computable {
                    PsiBuilderFactory.getInstance().createBuilder(
                        LanguageParserDefinitions.INSTANCE.forLanguage(BlueprintLanguage.INSTANCE),
                        FlexAdapter(BlueprintLexer()),
                        text
                    )
                }
            )
        }
    }
}