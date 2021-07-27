/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

fun File.getSystemIndependentPath(): String
        = FileUtil.toSystemIndependentName(absolutePath)

fun File.toFileSystemUrl(): String
        = StandardFileSystems.FILE_PROTOCOL_PREFIX + this.getSystemIndependentPath() + "/"

fun File.toJarFileUrl(): VirtualFile? {
    val url = JarFileSystem.PROTOCOL_PREFIX + this.getSystemIndependentPath() + JarFileSystem.JAR_SEPARATOR + "/"
    return VirtualFileManager.getInstance().findFileByUrl(url)
}

fun File.addClassesToLibrary(lib: Library.ModifiableModel) {
    toJarFileUrl()?.let {
        lib.addRoot(it, OrderRootType.CLASSES)
    }
}

fun File.addSourcesToLibrary(lib: Library.ModifiableModel) {
    toJarFileUrl()?.let {
        lib.addRoot(it, OrderRootType.SOURCES)
    }
}
