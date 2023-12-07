/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.parser

import com.github.pvoid.androidbp.blueprint.completion.BlueprintAutocompletion
import com.github.pvoid.androidbp.blueprint.completion.BlueprintField
import com.github.pvoid.androidbp.blueprint.completion.BlueprintObjectField
import com.github.pvoid.androidbp.blueprint.psi.BlueprintBlueprint
import com.github.pvoid.androidbp.blueprint.psi.BlueprintBlueprintType
import com.github.pvoid.androidbp.blueprint.psi.BlueprintFieldName
import com.github.pvoid.androidbp.blueprint.psi.BlueprintMembers
import com.github.pvoid.androidbp.blueprint.psi.BlueprintPair
import com.github.pvoid.androidbp.blueprint.psi.BlueprintStringExpr
import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.github.pvoid.androidbp.trimQuotes
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

object BlueprintPsiUtils {
    @JvmStatic
    fun getValue(element: BlueprintStringExpr): String =
        generateSequence(element.firstChild) { it.nextSibling }.filter {
            it.node.elementType == BlueprintTypes.STRING
        }.joinToString {
            it.text.trimQuotes()
        }

    @JvmStatic
    fun getBlueprintName(element: BlueprintBlueprint): String? {
        return PsiTreeUtil.collectElements(element) {
            it is BlueprintBlueprintType
        }.firstOrNull()?.text
    }

    @JvmStatic
    fun getFieldDef(element: BlueprintPair): BlueprintField? {
        val name = element.fieldName.text ?: return null

        val path = Stack<String>()
        var node = element.parent
        while (node != null) {
            if (node is BlueprintBlueprint) {
                node.blueprintName?.let(path::add)
                break
            }

            if (node is BlueprintPair) {
                path.add(node.fieldName.text)
            }

            node = node.parent
        }

        var fields: List<BlueprintField> = BlueprintAutocompletion.fields(path.pop())
        while (path.isNotEmpty()) {
            val fieldName = path.pop()
            val field = fields.firstOrNull { it.name == fieldName } ?: return null

            if (field !is BlueprintObjectField) {
                return null
            }

            fields = field.fields
        }

        return fields.firstOrNull { it.name == name }
    }
}