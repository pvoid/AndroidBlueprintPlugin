/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.util.PathString
import com.android.ide.common.util.toPathString
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.projectmodel.ExternalLibraryImpl
import com.android.projectmodel.RecursiveResourceFolder
import com.android.projectmodel.ResourceFolder
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.github.pvoid.androidbp.blueprint.DependenciesScope
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.io.File
import java.nio.file.Files

const val BUILD_CACHE_PATH = "out/soong/.intermediates/"

data class BlueprintExternalLibrary(
    override val address: String,
    override val assetsFolder: PathString?,
    override val hasResources: Boolean,
    override val location: PathString?,
    override val manifestFile: PathString?,
    override val packageName: String?,
    override val resApkFile: PathString?,
    override val resFolder: ResourceFolder?,
    override val symbolFile: PathString?,
    val jars: List<File>
) : ExternalAndroidLibrary

object LibrariesTools {
    fun getOutputPath(rootPath: File) = File(rootPath, BUILD_CACHE_PATH)

    fun createLibrary(table: LibraryTable, blueprint: Blueprint, blueprintsTable: BlueprintsTable, rootPath: File): Library? {
        if (blueprint.name == "kotlin-stdlib") {
            return WriteAction.computeAndWait<Library, Throwable> {
                var lib = table.libraries.firstOrNull { it.name == "KotlinJavaRuntime" }
                if (lib == null) {
                    lib = table.createLibrary("KotlinJavaRuntime")
                    configureKotlinDependency(lib, rootPath)
                }
                lib
            }
        }
        val outputPath = getOutputPath(rootPath)
        val jars = blueprint.outputJars(outputPath)
        if (jars.isEmpty()) {
            return null
        }

        val srcJars = mutableListOf<File>()
        val sources = mutableSetOf<File>()
        val queue = mutableListOf(blueprint)

        while (queue.isNotEmpty()) {
            val bp = queue.pop()

            if (bp.isJavaProject() || bp.isAndroidProject()) {
                bp.sources().mapNotNull {
                    if (it[0] == ':') {
                        blueprintsTable[it.substring(1)]?.let { link ->
                            queue.add(link)
                        }
                        null
                    } else {
                        File(rootPath, it)
                    }
                }.filter {
                    it.isDirectory
                }.toCollection(sources)
            }

            bp.generatedSources().map {
                File(outputPath, it)
            }.forEach {
                if (it.isDirectory) {
                    sources.add(it)
                } else if (it.extension == "jar") {
                    srcJars.add(it)
                } else if (it.extension == "srcjar") {
                    // ArchiveFileType is registered only for jars and not for srcjar
                    // Hack it by creating a link named .jar
                    val link = File(it.absolutePath + ".jar")
                    if (!link.exists()) {
                        Files.createLink(link.toPath(), it.toPath())
                    }
                    srcJars.add(link)
                }
            }

            bp.defaults().mapNotNull {
                blueprintsTable[it]
            }.toCollection(queue)

            bp.dependencies(DependenciesScope.Static).mapNotNull {
                blueprintsTable[it]
            }.toCollection(queue)
        }

        return WriteAction.computeAndWait<Library, Throwable> {
            val lib = table.getLibraryByName(blueprint.name) ?: table.createLibrary(blueprint.name)
            lib.modifiableModel.apply {
                // TODO: Drop entries
                jars.forEach {
                    it.addClassesToLibrary(this)
                }
                sources.forEach {
                    it.addSourcesToLibrary(this)
                }
                srcJars.forEach {
                    it.addSourcesJarToLibrary(this)
                }
                commit()
            }

            lib
        }
    }

    fun createAndroidLibrary(project: Project, dependency: AndroidDependencyRecord): BlueprintExternalLibrary? {
        val rootPath = project.guessAospRoot() ?: return null
        val outputPath = File(rootPath, BUILD_CACHE_PATH)
        val manifest = dependency.manifests.firstOrNull()?.toPathString() ?: return null
        val packageName = dependency.packageName ?: return null

        return if (dependency.apk.isNotEmpty()) {
            BlueprintExternalLibrary(
                address = dependency.name,
                manifestFile = manifest,
                packageName = packageName,
                resFolder = dependency.generatedRes.firstOrNull()?.let {
                    RecursiveResourceFolder(File(outputPath, it).toPathString())
                },
                symbolFile = dependency.R.firstOrNull()?.let { File(outputPath, it) }?.toPathString(),
                resApkFile = dependency.apk.firstOrNull()?.let { File(outputPath, it) }?.toPathString(),
                hasResources = true,
                jars = dependency.jars,
                location = null,
                assetsFolder = null
            )
        } else if (dependency.res.isNotEmpty()) {
            BlueprintExternalLibrary(
                address = dependency.name,
                resFolder = dependency.res.firstOrNull()?.let {
                    RecursiveResourceFolder(File(it).toPathString())
                },
                assetsFolder = dependency.assets.firstOrNull()?.let {File(outputPath, it) }?.toPathString(),
                manifestFile = manifest,
                packageName = packageName,
                hasResources = true,
                jars = dependency.jars,
                location = null,
                resApkFile = null,
                symbolFile = null
            )
        } else {
            null
        }
    }

    fun createAidlGenLibrary(project: Project, table: LibraryTable, blueprint: Blueprint): Library? {
        val aospRoot = project.guessAospRoot() ?: return null

        val aidls = blueprint.aidl_includes().map { aidl ->
            val src = File(aospRoot, "${BUILD_CACHE_PATH}/${blueprint.relativePath}/android_common/gen/aidl/${blueprint.relativePath}").parent.let {
                File(it, aidl)
            }
            val cls = File(aospRoot, "${BUILD_CACHE_PATH}/${blueprint.relativePath}/android_common/javac/classes/")
            cls to src
        }

        if (aidls.isEmpty()) {
            return null
        }

        return WriteAction.computeAndWait<Library, Throwable> {
            val name = "${blueprint.name}-aidl-gen"
            val lib = table.getLibraryByName(name) ?: table.createLibrary(name)
            lib.modifiableModel.apply {
                aidls.forEach { (cls, src) ->
                    cls.toVirtualFile()?.let {
                        this.addRoot(it, OrderRootType.CLASSES)
                    }
                    src.toVirtualFile()?.let {
                        this.addRoot(it, OrderRootType.SOURCES)
                    }
                }
                commit()
            }

            lib
        }
    }

    private fun configureKotlinDependency(library: Library, rootPath: File) {
        val path = File(rootPath, "external/kotlinc/lib/")
        if (!path.exists() || !path.isDirectory) {
            return
        }

        library.modifiableModel.apply {
            // Classes
            File(path, "kotlin-stdlib.jar").addClassesToLibrary(this)
            File(path, "kotlin-test.jar").addClassesToLibrary(this)
            File(path, "kotlin-reflect.jar").addClassesToLibrary(this)
            File(path, "kotlin-stdlib-jdk7.jar").addClassesToLibrary(this)
            File(path, "kotlin-stdlib-jdk8.jar").addClassesToLibrary(this)
            // Sources
            File(path, "kotlin-stdlib-sources.jar").addSourcesJarToLibrary(this)
            File(path, "kotlin-test-sources.jar").addClassesToLibrary(this)
            File(path, "kotlin-reflect-sources.jar").addSourcesJarToLibrary(this)
            File(path, "kotlin-stdlib-jdk7-sources.jar").addSourcesJarToLibrary(this)
            File(path, "kotlin-stdlib-jdk8-sources.jar").addSourcesJarToLibrary(this)

            commit()
        }
    }
}

private fun File.getSystemIndependentPath(): String
        = FileUtil.toSystemIndependentName(absolutePath)

private fun File.toFileSystemUrl(): String
        = StandardFileSystems.FILE_PROTOCOL_PREFIX + this.getSystemIndependentPath() + "/"


private fun File.toJarFileUrl(): VirtualFile? {
    val url = JarFileSystem.PROTOCOL_PREFIX + this.getSystemIndependentPath() + JarFileSystem.JAR_SEPARATOR + "/"
    return VirtualFileManager.getInstance().findFileByUrl(url)
}

private fun File.addClassesToLibrary(lib: Library.ModifiableModel) {
    toJarFileUrl()?.let {
        lib.addRoot(it, OrderRootType.CLASSES)
    }
}

private fun File.addSourcesJarToLibrary(lib: Library.ModifiableModel) {
    toJarFileUrl()?.let {
        lib.addRoot(it, OrderRootType.SOURCES)
    }
}

private fun File.addSourcesToLibrary(lib: Library.ModifiableModel) {
    toFileSystemUrl().let {
        VirtualFileManager.getInstance().findFileByUrl(it)
    }?.let {
        lib.addRoot(it, OrderRootType.SOURCES)
    }
}
