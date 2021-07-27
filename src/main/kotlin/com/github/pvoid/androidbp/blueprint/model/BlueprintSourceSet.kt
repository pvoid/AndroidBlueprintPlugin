/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import java.io.File

interface SourceSet {
    companion object : (String) -> SourceSet {
        override fun invoke(value: String): SourceSet = if (value.firstOrNull() == ':') {
            SourceLink(value)
        } else {
            GlobItem(value)
        }
    }
}

class GlobItem(pattern: String) : SourceSet {

    private val pattern: List<Pattern>

    init {
        var pos = 0
        val chunks = mutableListOf<Pattern>()

        while (true) {
            val next = pattern.indexOf('*', pos)
            if (next == -1) {
                break
            }
            chunks.add(ExactMatchPattern(pattern.substring(pos, next)))
            pos = next + 1
            if (pos < pattern.length && pattern[pos] == '*') {
                chunks.add(GreedyMatchPattern())
                pos += 1
            } else {
                chunks.add(NonGreedyMatchPattern())
            }
        }

        chunks.add(ExactMatchPattern(pattern.substring(pos)))
        this.pattern = chunks
    }

    fun isPattern(): Boolean = pattern.size > 1 || pattern.first() !is ExactMatchPattern

    fun isFolder(): Boolean {
        val part = pattern.asReversed().firstOrNull { it is ExactMatchPattern } as? ExactMatchPattern ?: return false
        val dotIndex = part.value.indexOf('.')
        val separatorIndex = part.value.indexOf('/')
        // TODO: Cover cases when the last part doesn't have both / and .
        return dotIndex < 0 || dotIndex < separatorIndex
    }

    fun fileExtension(): String? = pattern.lastOrNull()?.takeIf { it is ExactMatchPattern }?.let { pattern ->
        val file = (pattern as ExactMatchPattern).value
        val index = file.lastIndexOf('.')
        if (index < 0) null else file.substring(index + 1)
    }

    fun toFullPath(base: File): File = pattern.firstOrNull { it is ExactMatchPattern }?.let {  File(base, (it as ExactMatchPattern).value) } ?: base

    fun toRelativeString(): String = pattern.takeWhile { it is ExactMatchPattern }.joinToString { (it as ExactMatchPattern).value }

    private interface Pattern

    private class ExactMatchPattern(
        val value: String
    ) : Pattern

    private class GreedyMatchPattern : Pattern

    private class NonGreedyMatchPattern : Pattern
}

class SourceLink(pattern: String) : SourceSet {
    val library: String = pattern.substring(1)
}
