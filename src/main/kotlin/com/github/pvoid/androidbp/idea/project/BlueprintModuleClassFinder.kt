/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.SdkConstants
import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.idea.projectsystem.ClassContent
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.idea.util.toIoFile
import com.android.tools.idea.util.toVirtualFile
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.DependenciesScope
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object BlueprintClassJarProvider : ClassJarProvider {
    private var jars = emptyList<File>()

    override fun getModuleExternalLibraries(module: Module): List<File?> = jars

    fun updateExternalJars(module: Module, blueprints: Collection<Pair<DependenciesScope, Blueprint>>) {
        val aospPath = module.project.guessAospRoot() ?: return
        val rootPath = SoongTools.getOutputPath(aospPath)

        jars = blueprints.mapNotNull { (scope, blueprint) ->
            if (scope == DependenciesScope.Dynamic) {
                blueprint
            } else {
                null
            }
        }.flatMap { blueprint ->
            blueprint.outputJars(rootPath).filter { it.exists() }
        }
    }
}

class BlueprintModuleClassFinder(
    private val module: Module,
) : ClassFileFinder {
    override fun findClassFile(fqcn: String): ClassContent? {
        val aospPath = module.project.guessAospRoot() ?: return null
        val rootPath = SoongTools.getOutputPath(aospPath)
        val moduleSystem = module.getModuleSystem() as? BlueprintModuleSystem ?: return null

//        val logLines = mutableListOf("finder: Looking for $fqcn in module classes")

        return moduleSystem.blueprints.filter {
            it.isAndroidProject() || it.isJavaProject()
        }.flatMap { blueprint ->
            val result = mutableListOf<File>()

            rootPath.getPath( "${blueprint.relativePath}/android_common/combined/${blueprint.name}.jar").takeIf {
                it.exists()
            }?.let {
                result.add(it)
            }

            result
        }.mapNotNull {
            it.toVirtualFile()
        }.firstNotNullOfOrNull { jarFile ->
//            logLines.add("finder: $fqcn -> ${jarFile.canonicalPath}")
            findClassFileInOutputJar(jarFile, fqcn)?.let { content ->
                jarFile to content
            }
        }?.let { (file, content) ->
//            logLines.add("content size: ${content.size}")
            ClassContent.fromJarEntryContent(file.toIoFile(), content)
        }.also {
//            logLines.add("finder: ${if (it == null) "not found" else "found"}")
//            synchronized(this@BlueprintModuleClassFinder) {
//                logLines.forEach(Log::warn)
//            }
        }
    }

    private fun findClassFileInOutputJar(outputRoot: VirtualFile, fqcn: String): ByteArray? {
        if (!outputRoot.exists()) return null

        val pathSegments = fqcn.split(".").toTypedArray()
        pathSegments[pathSegments.size - 1] += SdkConstants.DOT_CLASS
        val outputBase = (JarFileSystem.getInstance().getJarRootForLocalFile(outputRoot) ?: outputRoot)

        val classFile = VfsUtil.findRelativeFile(outputBase, *pathSegments)
            ?: VfsUtil.findFile(Paths.get(outputBase.path, *pathSegments), true)

        if (classFile == null || !classFile.exists()) {
            return null
        }

        return try {
            with(classFile.inputStream) {
                readAllBytes()
            }
        } catch (e: IOException) {
            LOG.error("Class read failed", e)
            null
        }
    }
}