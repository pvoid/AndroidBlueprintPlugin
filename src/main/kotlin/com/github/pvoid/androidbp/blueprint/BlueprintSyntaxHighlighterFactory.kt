/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

private val VALUE_SEPARATOR: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "BLUEPRINT_VALUE_SEPARATOR",
    DefaultLanguageHighlighterColors.OPERATION_SIGN
)
private val ELEMENT_SEPARATOR: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_ITEM_SEPARATOR", DefaultLanguageHighlighterColors.COMMA)
private val BRACKETS: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
private val BLUEPRINT_TYPE: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
private val FIELD_NAME: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("FIELD_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
private val BOOL: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_BOOL", DefaultLanguageHighlighterColors.NUMBER)
private val STRING: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_STRING", DefaultLanguageHighlighterColors.STRING)
private val REFERENCE: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("VARIABLE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
private val COMMENT: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
private val BAD_CHARACTER: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
private val NUMBER: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("BLUEPRINT_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

private val BAD_CHAR_KEYS = arrayOf(BAD_CHARACTER)
private var BLUEPRINT_TYPE_KEYS = arrayOf(BLUEPRINT_TYPE)
private val VALUE_SEPARATOR_KEYS = arrayOf(VALUE_SEPARATOR)
private val ELEMENT_SEPARATOR_KEYS = arrayOf(ELEMENT_SEPARATOR)
private val FIELD_KEYS = arrayOf(FIELD_NAME)
private val NUMBERS_KEY = arrayOf(NUMBER)
private val STRING_KEYS = arrayOf(STRING)
private val BOOL_KEYS = arrayOf(BOOL)
private val COMMENT_KEYS = arrayOf(COMMENT)
private val BRACKETS_KEYS = arrayOf(BRACKETS)
private val REFERENCE_KEYS = arrayOf(REFERENCE)
private val EMPTY_KEYS = arrayOf<TextAttributesKey>()


private class BlueprintSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return FlexAdapter(BlueprintLexer())
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            BlueprintTypes.PLUS, BlueprintTypes.EQUALS, BlueprintTypes.PLUS_EQUALS -> VALUE_SEPARATOR_KEYS
            BlueprintTypes.ELEMENT_SEPARATOR -> ELEMENT_SEPARATOR_KEYS
            BlueprintTypes.OBJECT_START, BlueprintTypes.OBJECT_END, BlueprintTypes.ARRAY_START, BlueprintTypes.ARRAY_END -> BRACKETS_KEYS
            BlueprintTypes.COMMENT -> COMMENT_KEYS
            BlueprintTypes.BLUEPRINT_TYPE -> BLUEPRINT_TYPE_KEYS
            BlueprintTypes.FIELD_NAME -> FIELD_KEYS
            BlueprintTypes.STRING -> STRING_KEYS
            BlueprintTypes.BOOL -> BOOL_KEYS
            BlueprintTypes.NUMBER -> NUMBERS_KEY
            BlueprintTypes.LINK -> REFERENCE_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }
    }
}

class BlueprintSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return BlueprintSyntaxHighlighter()
    }
}