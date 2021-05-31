/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.intellij.codeInsight.completion.*
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

        document.insertString(context.tailOffset, " {\n    name: \"\",\n}")
        editor.caretModel.moveToOffset(context.tailOffset - 4)
    }
}
