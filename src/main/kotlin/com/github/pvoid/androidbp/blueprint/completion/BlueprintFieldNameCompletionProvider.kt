/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.github.pvoid.androidbp.blueprint.completion.fields.*
import com.github.pvoid.androidbp.blueprint.psi.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.backend.common.pop

class BlueprintFieldNameCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val parent = PsiTreeUtil.findFirstParent(parameters.position) {
            it is BlueprintMembers || it is BlueprintObject
        }

        if (parent is BlueprintObject) {
            addObjectFieldCompletion(parent, emptyList(), result)
            return
        }

        if (parent !is BlueprintMembers) {
            return
        }

        val blueprint = parent.parent
        val used = PsiTreeUtil.collectElements(parent) {
            it is BlueprintFieldName
        }.map {
            it.text
        }.toList()

        if (blueprint is BlueprintBlueprint) {
            addBlueprintFieldCompletion(blueprint, used, result)
        } else if (blueprint is BlueprintObject) {
            addObjectFieldCompletion(blueprint, used, result)
        }
    }

    private fun addBlueprintFieldCompletion(
        blueprint: BlueprintBlueprint,
        used: List<String>,
        result: CompletionResultSet
    ) {
        val name = blueprint.blueprintName ?: return
        BLUEPRINT_FIEDLS[name]?.filter {
            it.name !in used
        }?.forEach {
            result.addElement(
                LookupElementBuilder.create(it.name).withInsertHandler(BlueprintFieldInsertHandler(it, 1))
            )
        }
    }

    private fun addObjectFieldCompletion(obj: BlueprintObject, used: List<String>, result: CompletionResultSet) {
        val path = mutableListOf<String>()
        var node = obj.parent
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

        if (path.isEmpty()) {
            return
        }

        var fields: List<BlueprintField> = BLUEPRINT_FIEDLS[path.pop()] ?: return
        val ident = path.size
        while (path.isNotEmpty()) {
            val fieldName = path.pop()
            val field = fields.firstOrNull { it.name == fieldName } ?: return

            if (field !is BlueprintObjectField) {
                return
            }

            fields = field.fields
        }

        fields.filter {
            it.name !in used
        }.forEach {
            result.addElement(
                LookupElementBuilder.create(it.name).withInsertHandler(BlueprintFieldInsertHandler(it, ident))
            )
        }
    }
}

private class BlueprintFieldInsertHandler(
    private val mType: BlueprintField,
    private val mIdent: Int
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document

        val suffix = StringBuilder(": ")
        var caretStartOffset = 0
        var caretEndOffset = 0
        when (mType) {
            is BlueprintStringField, is BlueprintInterfaceField, is BlueprintReferenceField, is BlueprintLibraryField -> {
                suffix.append("\"\"")
                caretEndOffset = 1
                caretStartOffset = caretEndOffset
            }
            is BlueprintStringListField, is BlueprintLibrariesListField, is BlueprintReferencesListField -> {
                suffix.append("[  ]")
                caretEndOffset = 2
                caretStartOffset = caretEndOffset
            }
            is BlueprintObjectField -> {
                suffix.append("{\n")
                suffix.append("\n".padStart((mIdent + 1) * 4 + 1))
                suffix.append("}".padStart(mIdent * 4 + 1))
                caretEndOffset = mIdent * 4 + 2
                caretStartOffset = caretEndOffset
            }
            is BlueprintBooleanField -> {
                suffix.append("true")
                caretEndOffset = 0
                caretStartOffset = 4
            }
            is BlueprintNumberField -> {
                suffix.append("0")
                caretEndOffset = 0
                caretStartOffset = 1
            }
        }
        suffix.append(",")
        ++caretEndOffset
        ++caretStartOffset

        document.insertString(context.tailOffset, suffix)
        editor.caretModel.moveToOffset(context.tailOffset - caretEndOffset)
        if (caretStartOffset != caretEndOffset) {
            editor.selectionModel.setSelection(
                context.tailOffset - caretStartOffset,
                context.tailOffset - caretEndOffset
            )
        }
    }
}