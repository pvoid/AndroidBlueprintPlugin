/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.blueprint.parser.BlueprintParser
import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

val WHITE_SPACES = TokenSet.create(BlueprintTypes.WHITE_SPACE)
val COMMENTS = TokenSet.create(BlueprintTypes.COMMENT)
val STRINGS = TokenSet.create(BlueprintTypes.STRING)

val FILE = IFileElementType(BlueprintLanguage.INSTANCE)

class BlueprintFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BlueprintLanguage.INSTANCE) {

    override fun getFileType(): FileType = BlueprintFileType.INSTANCE

    override fun toString(): String = "Blueprint File"
}

class BlueprintParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer {
        return BlueprintLexerAdapter()
    }

    override fun createParser(project: Project?): PsiParser {
        return BlueprintParser()
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode?): PsiElement {
        return BlueprintTypes.Factory.createElement(node)
    }

    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return BlueprintFile(viewProvider)
    }

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements {
        return ParserDefinition.SpaceRequirements.MAY
    }
}