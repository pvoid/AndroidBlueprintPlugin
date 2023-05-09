/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.FileReader

private enum class VariableAction {
    Create,
    Append,
    Assign
}
private class BlueprintBuilder {
    private var name: String? = null
    private var type: String? = null
    private val values = mutableMapOf<String, Any>()

    fun type(type: String) {
        this.type = type
    }

    fun param(name: String, value: String, action: VariableAction) {
        when (name) {
            "LOCAL_MODULE" -> {
                this.name = value
            }
            "LOCAL_PACKAGE_NAME" -> {
                this.name = value
            }
            "LOCAL_STATIC_ANDROID_LIBRARIES" -> {
                assign("android_static_libs", value.split(' ').toMutableList(), action)
            }
            "LOCAL_STATIC_JAVA_LIBRARIES" -> {
                assign("java_static_libs", value.split(' ').toMutableList(), action)
            }
            "LOCAL_JAVA_LIBRARIES" -> {
                assign("libs", value.split(' ').toMutableList(), action)
            }
            "LOCAL_SRC_FILES" -> addSource(value, action)
            "LOCAL_RESOURCE_DIR" -> {
                assign("resource_dirs", value.split(' ').map(this::fixLocalDir).filterNot { it.isBlank() }.toMutableList(), action)
            }
//            else -> LOG.warn("$name=$value")
        }
    }

    private fun fixLocalDir(path: String): String {
        val parts = path.split('/').mapNotNull { part ->
            if (part.startsWith('$')) {
                if (part == "$(LOCAL_PATH)") {
                    null
                } else {
                    return ""
                }
            } else {
                part
            }
        }

        return parts.joinToString("/")
    }

    private fun assign(name: String, value: List<String>, action: VariableAction) {
        when (action) {
            VariableAction.Create -> {
                values[name] = value
            }
            VariableAction.Append -> {
                @Suppress("UNCHECKED_CAST")
                (values[name] as? MutableList<String>)?.addAll(value)
            }
            VariableAction.Assign -> {
                if (!values.containsKey("static_libs")) {
                    values[name] = value
                }
            }
        }
    }

    private fun addSource(value: String, action: VariableAction) {
        var start = 0
        val sources = mutableListOf<String>()

        while (start < value.length) {
            if (value[start] == '$') {
                if (value.startsWith("$(call ", start)) {
                    val end = value.indexOf(')', start)
                    val params = value.substring(7, end).split(',', limit = 2)
                    if (params.size == 2) {
                        val type = params[0].trim()
                        // Only java sources at the moment
                        if (type == "all-java-files-under") {
                            sources.add(params[1].trim())
                        }
                    }
                    start = end + 1
                } else {
                    start = value.indexOf(' ', start)
                    if (start == -1) {
                        start = value.length
                    }
                }
            } else {
                var end = value.indexOf(' ', start)
                if (end == -1) {
                    end = value.length
                }
                if (start != end) {
                    sources.add(value.substring(start, end))
                }
                start = end + 1
            }
        }

        assign("srcs", sources, action)
    }

    fun build(path: File, relativePath: String): Blueprint? {
        val name = this.name
        var type = this.type

        if (name == null || type == null) {
            return null
        }

        // Combine android and java static libraries
        val staticLibs = mutableListOf<String>()
        @Suppress("UNCHECKED_CAST")
        (values["android_static_libs"] as? List<String>)?.let(staticLibs::addAll)
        @Suppress("UNCHECKED_CAST")
        (values["java_static_libs"] as? List<String>)?.let(staticLibs::addAll)
        values["static_libs"] = staticLibs

        // Fix blueprint type
        if (type == BlueprintType.JavaLibraryStatic && values.containsKey("resource_dirs")) {
            type = BlueprintType.AndroidLibrary
        }

        return Blueprint(name, type, values, path, relativePath, true)
    }
}

object Makefile {
    const val DEFAULT_NAME = "Android.mk"

    private fun readMakefile(file: File, aospRoot: File, output: MutableList<String>) {
        val current = StringBuilder()

        FileReader(file).use { reader ->
            reader.readLines().asSequence()
                .map { it.trim() }
                .forEach { line ->
                    if (line.endsWith("\\")) {
                        current.append(line.subSequence(0, line.length - 1))
                    } else {
                        current.append(line)

                        if (current.startsWith("include ")) {
                            val value = current.substring(8).trimStart()
                            if (value[0] == '$') {
                                output.add(current.toString())
                            } else {
                                val include = File(aospRoot, value)
                                if (include.exists()) {
                                    readMakefile(include, aospRoot, output)
                                }
                            }
                        } else {
                            output.add(current.toString())
                        }
                        current.clear()
                    }
                }
        }
    }

    fun parse(file: File, aospRoot: File): List<Blueprint> {
        val lines = mutableListOf<String>()
        val result = mutableListOf<Blueprint>()
        var blueprint = BlueprintBuilder()
        val path = file.parentFile
        val relativePath = FileUtil.getRelativePath(aospRoot, path) ?: return result

        readMakefile(file, aospRoot, lines)

        lines.filterNot {
            it.isBlank() || it.startsWith('#')
        }.forEach { line ->
            if (line.startsWith("include ")) {
                val subject = line.split(' ', limit = 2)
                if (subject.size > 1) {
                    when (subject[1].trim()) {
                        "\$(CLEAR_VARS)" -> {
                            blueprint.build(path, relativePath)?.let(result::add)
                            blueprint = BlueprintBuilder()
                        }

                        "\$(BUILD_STATIC_JAVA_LIBRARY)" -> blueprint.type(BlueprintType.JavaLibraryStatic)
                        "\$(BUILD_PACKAGE)" -> blueprint.type(BlueprintType.AndroidApp)
                        "\$(BUILD_HOST_JAVA_LIBRARY)" -> blueprint.type(BlueprintType.JavaLibraryHost)
                    }
                }
            } else {
                val variable = line.split("=", limit = 2)
                if (variable.size == 2) {
                    lateinit var action: VariableAction
                    lateinit var name: String
                    when {
                        variable[0].last() == ':' -> {
                            name = variable[0].dropLast(1).trimEnd()
                            action = VariableAction.Create
                        }
                        variable[0].last() == '+' -> {
                            name = variable[0].dropLast(1).trimEnd()
                            action = VariableAction.Append
                        }
                        else -> {
                            name = variable[0].trimEnd()
                            action = VariableAction.Assign
                        }
                    }
                    blueprint.param(name, variable[1].trim(), action)
                }
            }
        }
        return result
    }
}
