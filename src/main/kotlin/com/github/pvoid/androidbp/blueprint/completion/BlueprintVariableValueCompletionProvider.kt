/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.github.pvoid.androidbp.blueprint.psi.BlueprintPair
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class BlueprintVariableValueCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val mLibraryInsertHandler = LibraryInsertHandler()

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val pair = PsiTreeUtil.getParentOfType(parameters.position, BlueprintPair::class.java) ?: return
        val field = pair.fieldDef ?: return

        if (field is BlueprintBooleanField) {
            result.addElement(LookupElementBuilder.create("true"))
            result.addElement(LookupElementBuilder.create("false"))
            return
        }

        if (field is BlueprintReferencesListField) {
            addLibrariesCompletion(parameters, result)
            return
        }
    }

    private fun addLibrariesCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val project = parameters.editor.project ?: return

        BlueprintsTable.getInstance(project).availableBlueprints().forEach {
            result.addElement(LookupElementBuilder.create(it).withInsertHandler(mLibraryInsertHandler))
        }
    }
}

private class LibraryInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document

        val addStartQuote = document.textLength > context.startOffset && document.text[context.startOffset - 1] != '"'
        val addEndQuote = document.textLength > context.tailOffset + 1 && document.text[context.tailOffset] != '"'
        var commaOffset = 0
        var addComma = true

        if (!addEndQuote) {
            var index = 1
            while (document.textLength > context.tailOffset + index && document.text[context.tailOffset + index].isWhitespace()) {
                ++index
            }

            if (document.textLength > context.tailOffset + index && document.text[context.tailOffset + index] == ',') {
                commaOffset = index
                addComma = false
            } else {
                commaOffset = 1
            }
        }

        if (addStartQuote) {
            document.insertString(context.startOffset, "\"")
        }

        if (addEndQuote) {
            document.insertString(context.tailOffset, "\"")
        }

        if (addComma) {
            document.insertString(context.tailOffset + commaOffset, ",")
        }

        if (!addEndQuote) {
            ++commaOffset
        }

        editor.caretModel.moveToOffset(context.tailOffset + commaOffset)
    }
}
