/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.SdkConstants
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.getModuleSystem
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.io.File
import java.nio.file.Paths

class BlueprintModuleClassFinder(
    private val module: Module,
) : ClassFileFinder {
    override fun findClassFile(fqcn: String): VirtualFile? {
        return findClassFileInModule(fqcn) ?: findClassInLibraries(fqcn)
    }

    private fun findClassFileInModule(fqcn: String): VirtualFile? {
        val rootPath = module.project.guessAospRoot() ?: return null
        val moduleSystem = module.getModuleSystem() as? BlueprintModuleSystem ?: return null

        return moduleSystem.blueprints.filter {
            it.isAndroidProject() || it.isJavaProject()
        }.flatMap {
            listOf(
                File(rootPath, "$BUILD_CACHE_PATH/${it.relativePath}/android_common/javac/classes"),
                File(rootPath, "$BUILD_CACHE_PATH/${it.relativePath}/android_common/kotlinc/classes"),
            )
        }.mapNotNull {
            it.toVirtualFile()
        }.firstNotNullOfOrNull {
            findClassFileInOutputRoot(it, fqcn).also {
                LOG.warn("Class: $fqcn found in ${it?.url}")
            }
        }
    }

    private fun findClassInLibraries(fqcn: String): VirtualFile? {
        return ModuleRootManager.getInstance(module).orderEntries.asSequence().filterIsInstance(LibraryOrderEntry::class.java).flatMap {
            it.getFiles(OrderRootType.CLASSES).asSequence()
        }.mapNotNull {
            findClassFileInOutputRoot(it, fqcn)
        }.firstOrNull()
    }

    private fun findClassFileInOutputRoot(outputRoot: VirtualFile, fqcn: String): VirtualFile? {
        if (!outputRoot.exists()) return null

        val pathSegments = fqcn.split(".").toTypedArray()
        pathSegments[pathSegments.size - 1] += SdkConstants.DOT_CLASS
        val outputBase = (JarFileSystem.getInstance().getJarRootForLocalFile(outputRoot) ?: outputRoot)

        val classFile = VfsUtil.findRelativeFile(outputBase, *pathSegments)
            ?: VfsUtil.findFile(Paths.get(outputBase.path, *pathSegments), true)

        return if (classFile != null && classFile.exists()) classFile else null
    }
}