/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.sdk

import java.io.File
import java.util.*

interface AospSdkBlueprints {
    fun findBlueprints(root: File): List<File>

    companion object : AospSdkBlueprints {
        private val mImpl = AospSdkBlueprintsImpl()

        override fun findBlueprints(root: File): List<File> = mImpl.findBlueprints(root)
    }
}

class AospSdkBlueprintsImpl : AospSdkBlueprints {
    override fun findBlueprints(root: File): List<File> {
        val folders = LinkedList<File>()
        val blueprints = mutableListOf<File>()
        folders.add(root)
        while (true) {
            val path = folders.pollFirst() ?: break
            path.list()?.forEach { fileName ->
                if (fileName != "." && fileName != "..") {
                    val file = File(path, fileName)
                    if (file.isDirectory) {
                        folders.addLast(file)
                    } else if (fileName == "Android.bp") {
                        blueprints.add(file)
                    }
                }
            }
        }

        return blueprints
    }
}
