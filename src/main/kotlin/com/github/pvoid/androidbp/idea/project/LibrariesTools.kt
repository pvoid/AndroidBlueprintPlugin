/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.ide.common.util.PathString
import com.android.ide.common.util.toPathString
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.projectmodel.RecursiveResourceFolder
import com.android.projectmodel.ResourceFolder
import com.android.tools.idea.util.toVirtualFile
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.github.pvoid.androidbp.blueprint.DependenciesScope
import com.github.pvoid.androidbp.idea.LOG
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.nio.file.Files
import java.util.*

// TODO: Make the list configurable
const val BUILD_CACHE_KATI_SYSTEM_PATH = "out_sys/target/common/obj/JAVA_LIBRARIES/"
const val BUILD_CACHE_VENDOR_PATH = "out/soong/.intermediates/"
const val BUILD_CACHE_KATI_VENDOR_PATH = "out/target/common/obj/JAVA_LIBRARIES/"
const val BUILD_CACHE_SYSTEM_PATH = "out_sys/soong/.intermediates/"

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
        val outputPath = SoongTools.getOutputPath(rootPath)
        val jars = blueprint.outputJars(outputPath)
        if (jars.isEmpty()) {
            return null
        }

        val srcJars = mutableListOf<File>()
        val sources = mutableSetOf<File>()
        val queue = Stack<Blueprint>()
        val processed = Stack<Blueprint>()

        queue.push(blueprint)
        while (queue.isNotEmpty()) {
            val bp = queue.pop()
            processed.add(bp)

            if (bp.isJavaProject() || bp.isAndroidProject()) {
                bp.sources().mapNotNull {
                    if (it.name[0] == ':') {
                        blueprintsTable[it.name.substring(1)]?.let { link ->
                            queue.add(link)
                        }
                        null
                    } else {
                        it
                    }
                }.filter {
                    it.isDirectory
                }.toCollection(sources)
            }

            bp.generatedSources(outputPath).forEach {
                if (!it.exists()) {
                    LOG.warn("Generated source archive $it does not exist for ${bp.name}")
                } else if (it.isDirectory) {
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
            }.filterNot {
                it in processed
            }.toCollection(queue)

            bp.dependencies(DependenciesScope.Static).mapNotNull {
                blueprintsTable[it]
            }.filterNot {
                it in processed
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

    fun createAndroidLibrary(dependency: AndroidDependencyRecord): BlueprintExternalLibrary? {
        val manifest = dependency.manifests.firstOrNull()?.toPathString() ?: return null
        val packageName = dependency.packageName ?: return null

        return if (dependency.apk.isNotEmpty()) {
            BlueprintExternalLibrary(
                address = dependency.name,
                manifestFile = manifest,
                packageName = packageName,
                resFolder = dependency.generatedRes.firstOrNull()?.let {
                    RecursiveResourceFolder(it.toPathString())
                },
                symbolFile = dependency.R.firstOrNull()?.toPathString(),
                resApkFile = dependency.apk.firstOrNull()?.toPathString(),
                hasResources = true,
                jars = dependency.jars,
                location = null,
                assetsFolder = null
            )
        } else if (dependency.res.isNotEmpty()) {
            BlueprintExternalLibrary(
                address = dependency.name,
                resFolder = dependency.res.firstOrNull()?.let {
                    RecursiveResourceFolder(it.toPathString())
                },
                assetsFolder = dependency.assets.firstOrNull()?.toPathString(),
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
        val outputPath = SoongTools.getOutputPath(aospRoot)

        val aidls = blueprint.aidl_includes_local().mapNotNull { aidl ->
            val src = outputPath.getPath("${blueprint.relativePath}/android_common/gen/aidl/${blueprint.relativePath}").parent.let {
                File(it, aidl)
            }
            val cls = outputPath.getPath("${blueprint.relativePath}/android_common/javac/classes/")
            cls.toVirtualFile()?.let {
                it to src
            }
        } + blueprint.aidl_includes_global().mapNotNull { aidl ->
            val src = outputPath.getPath("${blueprint.relativePath}/android_common/gen/aidl/$aidl")
            val cls = outputPath.getPath("${blueprint.relativePath}/android_common/javac/${blueprint.name}.jar")
            cls.toJarFileUrl()?.let {
                it to src
            }
        }

        if (aidls.isEmpty()) {
            return null
        }

        return WriteAction.computeAndWait<Library, Throwable> {
            val name = "${blueprint.name}-aidl-gen"
            val lib = table.getLibraryByName(name) ?: table.createLibrary(name)
            lib.modifiableModel.apply {
                aidls.forEach { (cls, src) ->
                    this.addRoot(cls, OrderRootType.CLASSES)

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
