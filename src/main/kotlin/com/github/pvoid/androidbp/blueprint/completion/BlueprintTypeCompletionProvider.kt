/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

class BlueprintTypeCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val mInsertHandler = BlueprintInsertHandler()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        BlueprintAutocompletion.blueprints.map { name ->
            LookupElementBuilder.create(name).withInsertHandler(mInsertHandler)
        }.forEach(result::addElement)
    }
}

private class BlueprintInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document

        // Looking for the next character
        var index = context.tailOffset
        if (index < document.textLength && (document.text[index].isLetter() || document.text[index] == '_')) {
            // Skip to first whitespace
            while (index < document.textLength && (document.text[index].isLetter() || document.text[index] == '_')) {
                ++index
            }
        }

        while (index < document.textLength && document.text[index].isWhitespace()) {
            ++index
        }

        if (index < document.textLength && document.text[index] == '{') {
            return
        }

        document.insertString(context.tailOffset, " {\n    name: \"\",\n}")
        editor.caretModel.moveToOffset(context.tailOffset - 4)
    }
}
