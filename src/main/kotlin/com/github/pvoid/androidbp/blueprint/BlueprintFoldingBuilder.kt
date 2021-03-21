/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.blueprint.psi.BlueprintBlueprint
import com.github.pvoid.androidbp.blueprint.psi.BlueprintMembers
import com.github.pvoid.androidbp.blueprint.psi.BlueprintPair
import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class BlueprintFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        return PsiTreeUtil.collectElementsOfType(root, BlueprintBlueprint::class.java).mapNotNull { blueprint ->
            blueprint.children.firstOrNull { it is BlueprintMembers }?.let { members ->
                val startOffset = generateSequence(members.node) {
                    it.treePrev
                }.firstOrNull {
                    it.elementType == BlueprintTypes.OBJECT_START
                }?.textRange?.endOffset ?: return@let null

                val endOffset = generateSequence(members.node) {
                    it.treeNext
                }.firstOrNull {
                    it.elementType == BlueprintTypes.OBJECT_END
                }?.textRange?.startOffset ?: return@let null

                FoldingDescriptor(blueprint.node, TextRange(startOffset, endOffset))
            }
        }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        val name = PsiTreeUtil.collectElementsOfType(node.psi, BlueprintPair::class.java).firstOrNull { it.fieldName.text == "name" }
        return name?.value?.text ?: "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}