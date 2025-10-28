/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.parser

import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

interface BlueprintSymbolFactory {
    fun <T: IElementType> create(type: Token, text: CharSequence): T
    fun skipSpaces(): Boolean

    enum class Token {
        ARRAY_END,
        OBJECT_START,
        PLUS,
        BLUEPRINT_TYPE,
        OBJECT_END,
        PLUS_EQUALS,
        VARIABLE_VALUE,
        VARIABLE_NAME,
        EQUALS,
        STRING,
        EOF,
        NUMBER,
        LINK,
        COMMENT,
        ERROR,
        BOOL,
        ELEMENT_SEPARATOR,
        FIELD_NAME,
        WHITE_SPACE,
        ARRAY_START;
    }

    companion object {
        @JvmStatic
        fun create() : BlueprintSymbolFactory = BlueprintIElementSymbolFactory()
    }
}

private class BlueprintIElementSymbolFactory : BlueprintSymbolFactory {
    override fun <T: IElementType> create(type: BlueprintSymbolFactory.Token, text: CharSequence): T = when (type) {
        BlueprintSymbolFactory.Token.COMMENT -> BlueprintTypes.COMMENT
        BlueprintSymbolFactory.Token.ARRAY_END -> BlueprintTypes.ARRAY_END
        BlueprintSymbolFactory.Token.OBJECT_START -> BlueprintTypes.OBJECT_START
        BlueprintSymbolFactory.Token.PLUS -> BlueprintTypes.PLUS
        BlueprintSymbolFactory.Token.BLUEPRINT_TYPE -> BlueprintTypes.BLUEPRINT_TYPE
        BlueprintSymbolFactory.Token.OBJECT_END -> BlueprintTypes.OBJECT_END
        BlueprintSymbolFactory.Token.VARIABLE_NAME -> BlueprintTypes.VARIABLE_NAME
        BlueprintSymbolFactory.Token.VARIABLE_VALUE -> BlueprintTypes.VARIABLE_VALUE
        BlueprintSymbolFactory.Token.EQUALS -> BlueprintTypes.EQUALS
        BlueprintSymbolFactory.Token.PLUS_EQUALS -> BlueprintTypes.PLUS_EQUALS
        BlueprintSymbolFactory.Token.STRING -> BlueprintTypes.STRING
        BlueprintSymbolFactory.Token.EOF -> BlueprintTypes.WHITE_SPACE
        BlueprintSymbolFactory.Token.NUMBER -> BlueprintTypes.NUMBER
        BlueprintSymbolFactory.Token.LINK -> BlueprintTypes.LINK
        BlueprintSymbolFactory.Token.ERROR -> TokenType.BAD_CHARACTER
        BlueprintSymbolFactory.Token.BOOL -> BlueprintTypes.BOOL
        BlueprintSymbolFactory.Token.ELEMENT_SEPARATOR -> BlueprintTypes.ELEMENT_SEPARATOR
        BlueprintSymbolFactory.Token.FIELD_NAME -> BlueprintTypes.FIELD_NAME
        BlueprintSymbolFactory.Token.WHITE_SPACE -> BlueprintTypes.WHITE_SPACE
        BlueprintSymbolFactory.Token.ARRAY_START -> BlueprintTypes.ARRAY_START
    } as T

    override fun skipSpaces(): Boolean = false
}
