/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.github.pvoid.androidbp.blueprint.psi.*
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.daemon.common.trimQuotes
import org.jetbrains.kotlin.psi.psiUtil.children
import java.io.File

fun ASTNode.toBlueprints(path: File): List<Blueprint> {
    val result = mutableListOf<Blueprint>()
    val factory = BlueprintsFactory()
    val variables = mutableMapOf<String, Any>()

    children().forEach { node ->
        when (node.elementType) {
            BlueprintTypes.BLUEPRINT -> createBlueprint(node.psi as BlueprintBlueprint, path, variables, factory)?.let(result::add)
            BlueprintTypes.VARIABLE -> createVariable(node.psi as BlueprintVariable, variables)
        }
    }
    return result
}

private fun createBlueprint(element: BlueprintBlueprint, path: File, variables: Map<String, Any>, factory: BlueprintsFactory): Blueprint? {
    val nameNode = element.children.firstOrNull {
        it.elementType == BlueprintTypes.BLUEPRINT_TYPE
    }?.text ?: return null

    val members = element.members?.let {
        createObject(it, variables)
    } ?: emptyMap()

    return factory.create(nameNode, members, path)
}

private fun createVariable(node: BlueprintVariable, table: MutableMap<String, Any>) {
    val name = node.node.findChildByType(BlueprintTypes.VARIABLE_NAME)
    var value: Any
    var doAdd = false
    var current = name?.psi ?: return // NOTE: It's an error actually

    while (true) {
        when (current.elementType) {
            BlueprintTypes.EQUALS -> break
            BlueprintTypes.PLUS_EQUALS -> {
                doAdd = true
                break
            }
        }
        current = current.nextSibling ?: return
    }

    while (true) {
        if (current.elementType == BlueprintTypes.VALUE) {
            value = nodeToValue(current.firstChild.nextValueNode(), table)
            break;
        }
        current = current.nextSibling ?: return
    }

    if (doAdd && table.containsKey(name.text)) {
        table[name.text] = concat(table[name.text], value)
    } else {
        table[name.text] = value
    }
}

private fun createObject(members: BlueprintMembers, variables: Map<String, Any>): Map<String, Any> {
    return members.pairList.fold(mutableMapOf()) { map, pair ->
        val value = pair.value.firstChild.nextValueNode()
        map[pair.fieldName.text] = nodeToValue(value, variables)
        map
    }
}

private fun fillArray(array: MutableList<Any>, elements: BlueprintElements, variables: Map<String, Any>) {
    elements.arrayElementList.map { item ->
        nodeToValue(item.firstChild.nextValueNode(), variables)
    }.fold(array) { arr, item ->
        arr.add(item)
        arr
    }
}

private fun nodeToValue(value: PsiElement?, variables: Map<String, Any>): Any = when (value?.elementType) {
    BlueprintTypes.NUMBER -> value?.text?.toIntOrNull()
    BlueprintTypes.STRING_EXPR -> (value as BlueprintStringExpr).value
    BlueprintTypes.STRING -> value?.text?.trimQuotes()
    BlueprintTypes.BOOL -> value?.text?.trim() == "true"
    BlueprintTypes.OBJECT -> (value as? BlueprintObject)?.members?.let { createObject(it, variables) }
    BlueprintTypes.ARRAY_EXPR -> {
        (value as? BlueprintArrayExpr)?.arrayList?.fold(mutableListOf<Any>()) { acc, item ->
            item.elements?.let {
                fillArray(acc, it, variables)
            }
            acc
        }
    }
    BlueprintTypes.LINK -> value?.text?.trimQuotes()
    BlueprintTypes.VARIABLE_REF_EXPR -> value?.text?.let { variables[it] }
    else -> null
} ?: Any()

private fun PsiElement.nextValueNode(): PsiElement? {
    var element: PsiElement? = this
    do {
        when (element.elementType) {
            BlueprintTypes.STRING_EXPR -> return element
            BlueprintTypes.STRING -> return element
            BlueprintTypes.ARRAY_EXPR -> return element
            BlueprintTypes.OBJECT -> return element
            BlueprintTypes.BOOL -> return element
            BlueprintTypes.NUMBER -> return element
            BlueprintTypes.LINK -> return element
            BlueprintTypes.VARIABLE_REF_EXPR -> return element
            else -> element = element?.nextSibling
        }
    } while (element != null)
    return null
}

private fun concat(left: Any?, right: Any): Any = when (left) {
    null -> right
    is List<*> -> {
        if (right !is List<*>) {
            throw RuntimeException("Can't concatenate array and non array"/*, reader.column, reader.line*/)
        }
        left + right
    }
    is String -> {
        if (right !is String) {
            throw RuntimeException("Can't concatenate string and non string"/*, reader.column, reader.line*/)
        }
        left + right
    }
    is Int -> {
        if (right !is Int) {
            throw RuntimeException("Can't concatenate int and non int"/*, reader.column, reader.line*/)
        }
        left + right
    }
    else -> {
        throw RuntimeException("Can't concatenate values"/*, reader.column, reader.line*/)
    }
}
