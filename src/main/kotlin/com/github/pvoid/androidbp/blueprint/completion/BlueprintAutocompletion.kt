/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class BlueprintInfo(
    val name: String,
    val desc: String,
    val fields: List<BlueprintField>
)

interface BlueprintAutocompletion {

    val blueprints: Sequence<String>

    fun fields(name: String): List<BlueprintField>

    fun load()

    companion object : BlueprintAutocompletion {

        private var mBlueprints: List<BlueprintInfo> = emptyList()

        override val blueprints: Sequence<String>
            get() = mBlueprints.asSequence().map { it.name }

        override fun load() {
            if (mBlueprints.isNotEmpty()) {
                // We can load it only once. No need to do it once again
                return
            }

            ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading autocompletion info") {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        val reader = FieldsXmlReader()
                        mBlueprints = reader.read()
                    } catch (e: FieldsXmlParseError) {
                        LOG.error("Autocompletion info parsing error", e)
                    }
                }
            })
        }

        override fun fields(name: String): List<BlueprintField>
            = mBlueprints.firstOrNull { it.name == name }?.fields ?: emptyList()
    }
}
