/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes.*
import com.intellij.psi.TokenType.BAD_CHARACTER

interface BlueprintSymbolFactory {
    fun <T> create(type: Token, text: CharSequence): T
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
}

@Suppress("UNCHECKED_CAST")
class BlueprintIElementSymbolFactory : BlueprintSymbolFactory {
    override fun <T> create(type: BlueprintSymbolFactory.Token, text: CharSequence): T = when (type) {
        BlueprintSymbolFactory.Token.COMMENT -> COMMENT
        BlueprintSymbolFactory.Token.ARRAY_END -> ARRAY_END
        BlueprintSymbolFactory.Token.OBJECT_START -> OBJECT_START
        BlueprintSymbolFactory.Token.PLUS -> PLUS
        BlueprintSymbolFactory.Token.BLUEPRINT_TYPE -> BLUEPRINT_TYPE
        BlueprintSymbolFactory.Token.OBJECT_END -> OBJECT_END
        BlueprintSymbolFactory.Token.VARIABLE_NAME -> VARIABLE_NAME
        BlueprintSymbolFactory.Token.VARIABLE_VALUE -> VARIABLE_VALUE
        BlueprintSymbolFactory.Token.EQUALS -> EQUALS
        BlueprintSymbolFactory.Token.PLUS_EQUALS -> PLUS_EQUALS
        BlueprintSymbolFactory.Token.STRING -> STRING
        BlueprintSymbolFactory.Token.EOF -> WHITE_SPACE
        BlueprintSymbolFactory.Token.NUMBER -> NUMBER
        BlueprintSymbolFactory.Token.LINK -> LINK
        BlueprintSymbolFactory.Token.ERROR -> BAD_CHARACTER
        BlueprintSymbolFactory.Token.BOOL -> BOOL
        BlueprintSymbolFactory.Token.ELEMENT_SEPARATOR -> ELEMENT_SEPARATOR
        BlueprintSymbolFactory.Token.FIELD_NAME -> FIELD_NAME
        BlueprintSymbolFactory.Token.WHITE_SPACE -> WHITE_SPACE
        BlueprintSymbolFactory.Token.ARRAY_START -> ARRAY_START
        else -> throw RuntimeException("Unexpected symbol #$type")
    } as T

    override fun skipSpaces(): Boolean = false
}
