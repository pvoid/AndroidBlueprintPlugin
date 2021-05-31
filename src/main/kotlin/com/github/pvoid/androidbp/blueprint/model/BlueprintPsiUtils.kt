/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.github.pvoid.androidbp.blueprint.completion.BlueprintAutocompletion
import com.github.pvoid.androidbp.blueprint.completion.BlueprintField
import com.github.pvoid.androidbp.blueprint.completion.BlueprintObjectField
import com.github.pvoid.androidbp.blueprint.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.backend.common.pop

object BlueprintPsiUtils {
    fun getValue(element: BlueprintStringExpr): String =
        element.children.filter {
            it.node.elementType == BlueprintTypes.STRING
        }.joinToString {
            it.text
        }

    fun getBlueprintName(element: BlueprintBlueprint): String? {
        return PsiTreeUtil.collectElements(element) {
            it is BlueprintBlueprintType
        }.firstOrNull()?.text
    }

    fun isBlueprintField(element: BlueprintFieldName): Boolean {
        return getFieldBlueprint(element) != null
    }

    fun getFieldBlueprint(element: BlueprintFieldName): BlueprintBlueprint? {
        val members = PsiTreeUtil.getParentOfType(element, BlueprintMembers::class.java) ?: return null
        val parent = members.parent

        if (parent !is BlueprintBlueprint) {
            return null
        }

        return parent
    }

    fun getFieldDef(element: BlueprintPair): BlueprintField? {
        val name = element.fieldName.text ?: return null

        val path = mutableListOf<String>()
        var node = element.parent
        while (node != null) {
            if (node is BlueprintBlueprint) {
                path.add(node.blueprintName)
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
