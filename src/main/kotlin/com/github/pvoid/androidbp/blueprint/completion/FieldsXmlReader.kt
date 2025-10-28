/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.intellij.util.ResourceUtil
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.streams.toList

class FieldsXmlReader {

    fun read(): List<BlueprintInfo> {
        val inp = ResourceUtil.getResourceAsStream(FieldsXmlReader::class.java.classLoader, "autocompletion", "blueprints.xml")
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inp, Charset.defaultCharset().name())

        val blueprints = mutableListOf<BlueprintInfo>()
        val stack = Stack<Pair<String, MutableList<BlueprintField>>>()

        generateSequence {
            when (val token = parser.nextToken()) {
                XmlPullParser.END_DOCUMENT -> null
                else -> token to parser
            }
        }.forEach { (token, parser) ->
            if (token == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "blueprints" -> {}
                    "blueprint" -> {
                        val name = parser.getAttributeValue(null, "name")
                            ?: throw FieldsXmlParseError("Missing blueprint name at ${parser.lineNumber}")
                        stack.add(name to mutableListOf())
                    }
                    "field" -> {
                        val name = parser.getAttributeValue(null, "name")
                            ?: throw FieldsXmlParseError("Missing field name at line ${parser.lineNumber}")
                        val type = parser.getAttributeValue(null, "type")
                            ?: throw FieldsXmlParseError("Missing field type for field [$name] at line ${parser.lineNumber}")
                        val desc = parser.getAttributeValue(null, "descr") ?: ""
                        val field = when (type) {
                            "string" -> BlueprintStringField(name, desc)
                            "string[]" -> BlueprintStringListField(name, desc)
                            "object[]" -> BlueprintObjectListField(name, desc)
                            "bool" -> BlueprintBooleanField(name, desc)
                            "number" -> BlueprintNumberField(name, desc)
                            "blueprint[]" -> BlueprintReferencesListField(name, desc)
                            "object" -> BlueprintObjectField(name, desc, emptyList())
                            else -> throw FieldsXmlParseError("Unsupported field type [$type] at ${parser.lineNumber}")
                        }
                        stack.last().second.add(field)
                    }
                    "object-field" -> {
                        val name = parser.getAttributeValue(null, "name")
                            ?: throw FieldsXmlParseError("Missing object-field name at ${parser.lineNumber}")
                        stack.add(name to mutableListOf())
                    }
                    else -> throw FieldsXmlParseError("Unsupported tag ${parser.name} at ${parser.lineNumber}")
                }
            } else if (token == XmlPullParser.END_TAG) {
                when (parser.name) {
                    "blueprints" -> {
                    }
                    "blueprint" -> {
                        val info = stack.pop()
                        blueprints.add(BlueprintInfo(info.first, "", info.second))
                    }
                    "field" -> {}
                    "object-field" -> {
                        val info = stack.pop()
                        stack.lastElement().second.add(BlueprintObjectField(info.first, "", info.second))
                    }
                }
            }
        }

        return blueprints
    }

}

class FieldsXmlParseError(msg: String) : RuntimeException(msg)