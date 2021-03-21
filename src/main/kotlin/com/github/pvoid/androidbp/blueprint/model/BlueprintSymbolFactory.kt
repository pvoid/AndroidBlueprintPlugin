/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes.*
import com.intellij.psi.TokenType.BAD_CHARACTER
import com.intellij.util.containers.Stack
import java_cup.runtime.Symbol
import java.io.File

interface BlueprintSymbolFactory {
    fun <T> create(type: Int, text: CharSequence): T
    fun skipSpaces(): Boolean
}

@Suppress("UNCHECKED_CAST")
class BlueprintIElementSymbolFactory : BlueprintSymbolFactory {
    override fun <T> create(type: Int, text: CharSequence): T = when (type) {
        BlueprintParserSym.COMMENT -> COMMENT
        BlueprintParserSym.ARRAY_END -> ARRAY_END
        BlueprintParserSym.OBJECT_START -> OBJECT_START
        BlueprintParserSym.PLUS -> PLUS
        BlueprintParserSym.BLUEPRINT_TYPE -> BLUEPRINT_TYPE
        BlueprintParserSym.OBJECT_END -> OBJECT_END
        BlueprintParserSym.VARIABLE_NAME -> VARIABLE_NAME
        BlueprintParserSym.VARIABLE_VALUE -> VARIABLE_VALUE
        BlueprintParserSym.EQUALS -> EQUALS
        BlueprintParserSym.PLUS_EQUALS -> PLUS_EQUALS
        BlueprintParserSym.STRING -> STRING
        BlueprintParserSym.EOF -> WHITE_SPACE
        BlueprintParserSym.NUMBER -> NUMBER
        BlueprintParserSym.LINK -> LINK
        BlueprintParserSym.error -> BAD_CHARACTER
        BlueprintParserSym.BOOL -> BOOL
        BlueprintParserSym.ELEMENT_SEPARATOR -> ELEMENT_SEPARATOR
        BlueprintParserSym.FIELD_NAME -> FIELD_NAME
        BlueprintParserSym.WHITE_SPACE -> WHITE_SPACE
        BlueprintParserSym.ARRAY_START -> ARRAY_START
        else -> throw RuntimeException("Unexpected symbol #$type")
    } as T

    override fun skipSpaces(): Boolean = false
}

class BlueprintCupSymbolFactory(
    private val mPath: File
) : BlueprintSymbolFactory {

    private val mBlueprints = mutableListOf<Blueprint>()

    private val mFactory = BlueprintsFactory()

    private var mDraftName: CharSequence? = null
    private var mVariableName: CharSequence? = null

    private val mCollectors = Stack<IValuesCollector>()

    private val mVariables = mutableMapOf<CharSequence, Any>()

    val blueprints: List<Blueprint>
        get() = mBlueprints

    @Suppress("UNCHECKED_CAST")
    override fun <T> create(type: Int, text: CharSequence): T {
        when (type) {
            BlueprintParserSym.BLUEPRINT_TYPE -> {
                if (mCollectors.isNotEmpty()) {
                    if (mCollectors.size != 1) {
                        throw AssertionError()
                    }

                    val last = mCollectors.pop()
                    if (last !is BlueprintVariable) {
                        throw AssertionError()
                    }

                    mVariables[last.name] = last.toValue()
                }

                mDraftName = text
            }

            BlueprintParserSym.OBJECT_START -> {
                mCollectors.push(mDraftName?.let {
                    BlueprintDraft(it)
                } ?: BlueprintObject())
                mDraftName = null
            }
            BlueprintParserSym.OBJECT_END -> {
                val last = mCollectors.pop()
                if (last is BlueprintDraft) {
                    mFactory.create(last.name, last.toValue() as Map<String, Any>, mPath)?.let(mBlueprints::add)
                } else {
                    mCollectors.peek().put(last.toValue())
                }
            }

            BlueprintParserSym.ARRAY_START -> mCollectors.push(BlueprintArray())
            BlueprintParserSym.ARRAY_END -> {
                val last = mCollectors.pop()
                if (last !is BlueprintArray) {
                    throw AssertionError()
                }
                mCollectors.peek().put(last.toValue())
            }

            BlueprintParserSym.VARIABLE_NAME -> {
                if (mCollectors.isNotEmpty()) {
                    if (mCollectors.size != 1) {
                        throw AssertionError()
                    }

                    val last = mCollectors.pop()
                    if (last !is BlueprintVariable) {
                        throw AssertionError()
                    }

                    mVariables[last.name] = last.toValue()
                }

                mVariableName = text
            }
            BlueprintParserSym.EQUALS -> {
                if (text == "=") {
                    mVariableName?.let {
                        mCollectors.push(BlueprintVariable(it))
                    } ?: throw AssertionError()
                    mVariableName = null
                }
            }
            BlueprintParserSym.PLUS_EQUALS -> {
                mVariableName?.let {
                    val variable = BlueprintVariable(it, mVariables[it])
                    variable.plus()
                    mCollectors.push(variable)
                } ?: throw AssertionError()
                mVariableName = null
            }
            BlueprintParserSym.VARIABLE_VALUE -> {
                mVariables[text]?.let {
                    mCollectors.peek().put(it)
                } ?: throw RuntimeException("Unknown variable [$text]")
            }

            BlueprintParserSym.FIELD_NAME -> mCollectors.peek().key(text)
            BlueprintParserSym.NUMBER -> {
                val value = try {
                    text.toString().toInt()
                } catch (_:NumberFormatException) {
                    0
                }
                mCollectors.peek().put(value)
            }
            BlueprintParserSym.PLUS -> mCollectors.peek().plus()
            BlueprintParserSym.BOOL -> mCollectors.peek().put(text == "true")
            BlueprintParserSym.STRING -> mCollectors.peek().put(text.drop(1).dropLast(1))
            BlueprintParserSym.LINK -> mCollectors.peek().put(text.drop(1).dropLast(1))
        }

        return Symbol(type) as T
    }

    override fun skipSpaces(): Boolean = true
}

private interface IValuesCollector {
    fun key(key: CharSequence)
    fun put(value: Any)
    fun plus()
    fun toValue(): Any
}

private class BlueprintArray : IValuesCollector {

    private val mValue = mutableListOf<Any>()

    override fun key(key: CharSequence) {
        throw AssertionError()
    }

    override fun put(value: Any) {
        mValue.add(value)
    }

    override fun plus() {
        throw AssertionError()
    }

    override fun toValue(): Any {
        return mValue
    }
}

private open class BlueprintObject : IValuesCollector {
    private var addNext: Boolean = false

    private val mValue = mutableListOf<ModifiablePair<CharSequence, Any?>>()

    override fun key(key: CharSequence) {
        mValue.add(ModifiablePair(key, null))
    }

    override fun put(value: Any) {
        if (!addNext) {
            mValue.last().second = value
        } else {
            mValue.last().second = concatValues(mValue.last().second, value)
            addNext = false
        }
    }

    override fun plus() {
        addNext = true
    }

    override fun toValue(): Any = mValue.filter { (_, value) -> value != null }.fold(mutableMapOf<CharSequence, Any>()) { acc, item ->
        acc[item.first] = item.second!!
        acc
    }
}

private class BlueprintDraft(
    val name: CharSequence
) : BlueprintObject()

private class BlueprintVariable(
    val name: CharSequence,
    private var mValue: Any? = null
) : IValuesCollector {

    private var addNext: Boolean = false

    override fun key(key: CharSequence) {
        throw AssertionError()
    }

    override fun put(value: Any) {
        if (addNext) {
            mValue = concatValues(mValue, value)
        } else if (mValue == null) {
            mValue = value
        } else {
            throw AssertionError()
        }
    }

    override fun plus() {
        addNext = true
    }

    override fun toValue(): Any = mValue!!
}

private fun concatValues(current: Any?, new: Any): Any = when (current) {
    null -> new
    is List<*> -> {
        if (new !is List<*>) {
            throw RuntimeException("Can't concatenate array and non array"/*, reader.column, reader.line*/)
        }
        current + new
    }
    is String -> {
        if (new !is String) {
            throw RuntimeException("Can't concatenate string and non string"/*, reader.column, reader.line*/)
        }
        current + new
    }
    is Int -> {
        if (new !is Int) {
            throw RuntimeException("Can't concatenate int and non int"/*, reader.column, reader.line*/)
        }
        current + new
    }
    else -> {
        throw RuntimeException("Can't concatenate values"/*, reader.column, reader.line*/)
    }
}

private data class ModifiablePair<out A, B>(
    val first: A,
    var second: B
)