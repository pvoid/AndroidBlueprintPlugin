/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices
import java.io.File

class OutputPaths(rootPath: File) {

    private val outputs = listOf(
        File(rootPath, BUILD_CACHE_VENDOR_PATH),
        File(rootPath, BUILD_CACHE_SYSTEM_PATH),
        File(rootPath, BUILD_CACHE_KATI_VENDOR_PATH),
        File(rootPath, BUILD_CACHE_KATI_SYSTEM_PATH),
    )

    fun getPath(relativePath: String): File {
        return outputs.firstNotNullOfOrNull { currentRoot ->
            val file = File(currentRoot, relativePath)
            if (file.exists()) {
                file
            } else {
                var result: File? = null
                val path = StringBuilder(currentRoot.absolutePath)
                var start = 0
                for (index in 0 until relativePath.length) {
                    if (relativePath[index] != '/') {
                        continue
                    }

                    val name = relativePath.substring(start, index)
                    if (File(path.toString(), name).exists()) {
                        path.append('/').append(name)
                        start = index + 1
                        continue
                    }

                    // Find if there is a hash
                    val prfx = File(path.toString())
                    result = prfx.list()?.filter { name ->
                        name.isHexString()
                    }?.map { hash ->
                        File(prfx, hash + '/' + relativePath.substring(start))
                    }?.firstOrNull {
                        it.exists()
                    }

                    break
                }
                result
            }
        } ?: File(outputs[0], relativePath)
    }
}

fun String.isHexString(): Boolean = this.all {
    it.isDigit() || (it in 'a'..'f')
}

object SoongTools {
    fun getOutputPath(rootPath: File) = OutputPaths(rootPath)

    fun subscribeBuildListener(listener: BuildSystemFilePreviewServices.BuildListener) {
        // NOTE: Preview wants to know when the class was rebuilt and expects that it happens in background
        // while editing happens (thank you compose). We don't know how to build the project so can't implement it,
        // but at least in the future can monitor the file system
    }
}