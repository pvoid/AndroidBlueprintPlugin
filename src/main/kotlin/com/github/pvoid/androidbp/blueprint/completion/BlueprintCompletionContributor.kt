/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.intellij.codeInsight.completion.*
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns

class BlueprintCompletionContributor : CompletionContributor(), DumbAware {
    init {
        BlueprintAutocompletion.load()

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(BlueprintTypes.VARIABLE_NAME), BlueprintTypeCompletionProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(BlueprintTypes.FIELD_NAME), BlueprintFieldNameCompletionProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(BlueprintTypes.VARIABLE_VALUE), BlueprintVariableValueCompletionProvider())
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(BlueprintTypes.STRING), BlueprintVariableValueCompletionProvider())
    }
}

