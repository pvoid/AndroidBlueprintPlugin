/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.intellij.psi.tree.IElementType

class BlueprintTokenType(debugName: String) : IElementType(debugName, BlueprintLanguage.INSTANCE) {
    override fun toString(): String = "BlueprintToken.${super.toString()}"
}

class BlueprintElementType(debugName: String) : IElementType(debugName, BlueprintLanguage.INSTANCE) {
}