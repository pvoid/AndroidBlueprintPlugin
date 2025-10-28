/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.android.ide.common.xml.AndroidManifestParser
import com.github.pvoid.androidbp.idea.project.OutputPaths
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.tree.IElementType
import java.io.File

object BlueprintType {
    const val AndroidApp = "android_app"
    const val AndroidLibrary = "android_library"
    const val AndroidImport = "android_library_import"
    const val JavaBinary = "java_binary"
    const val JavaBinaryHost = "java_binary_host"
    const val JavaLibrary = "java_library"
    const val JavaLibraryHost = "java_library_host"
    const val JavaLibraryStatic = "java_library_static"
    const val JavaSdk = "java_sdk_library"
    const val JavaImport = "java_import"
    const val JavaImportHost = "java_import_host"
    const val SyspropLibrary = "sysprop_library"
    const val AidlInterface = "aidl_interface"
    const val HidlInterface = "hidl_interface"
}

private val ANDROID_TYPES = arrayOf(BlueprintType.AndroidApp, BlueprintType.AndroidLibrary, "android_test", "android_test_helper_app")
private val ANDROID_IMPORT_TYPES = arrayOf(BlueprintType.AndroidImport)
private val JAVA_TYPES = arrayOf(BlueprintType.JavaBinary, BlueprintType.JavaBinaryHost, BlueprintType.JavaLibrary,
    BlueprintType.JavaLibraryHost, BlueprintType.JavaLibraryStatic, "java_plugin", BlueprintType.JavaSdk,
    "java_test", "java_test_helper_library", "java_defaults")
private val JAVA_IMPORT_TYPES = arrayOf(BlueprintType.JavaImport, BlueprintType.JavaImportHost)

private class PackageNameCache(
    val packageName: String,
    val fileName: String,
    val timestamp: Long
)

enum class DependenciesScope {
    All,
    Static,
    Dynamic
}

class Blueprint(
    val name: String,
    val type: String,
    private val members: Map<String, Any>,
    val path: File,
    private val relativePath_: String,
    val isFromKati: Boolean,
) {
    private var packageName: PackageNameCache? = null

    val relativePath: String = "$relativePath_/$name"

    fun isAndroidProject(): Boolean {
        return type in ANDROID_TYPES
    }

    fun isAndroidImport(): Boolean {
        return type in ANDROID_IMPORT_TYPES
    }

    fun isJavaProject(): Boolean {
        return type in JAVA_TYPES
    }

    fun isJavaImport(): Boolean {
        return type in JAVA_TYPES
    }

    fun manifest(): File? {
        if (type !in ANDROID_TYPES) {
            return null
        }

        // If the property is not set soong tries blueprint folder
        // with the default name
        val manifestFile = File(
            path,
            members["manifest"] as? String ?: "AndroidManifest.xml"
        )

        return if (manifestFile.exists()) {
            manifestFile
        } else {
            null
        }
    }

    fun resApk(): String? = when (type) {
        BlueprintType.AndroidImport -> "$relativePath_/$name/android_common/package-res.apk"
        else -> null
    }

    fun R(): String? {
        if (type !in ANDROID_TYPES) {
            return null
        }

        return if (!isFromKati) {
            "$relativePath_/$name/android_common/R.txt"
        } else {
            "../../target/common/obj/JAVA_LIBRARIES/${name}_intermediates/R.txt"
        }
    }

    fun packageName(): String? {
        val manifestFile = manifest() ?: return null
        val cache = packageName?.takeIf {
            it.fileName == manifestFile.absolutePath &&
            it.timestamp >= manifestFile.lastModified()
        }

        if (cache != null) {
            return cache.packageName
        }

        val result = AndroidManifestParser.parse(manifestFile.toPath())

        packageName = PackageNameCache(
            result.`package`,
            manifestFile.absolutePath,
            manifestFile.lastModified()
        )

        return result.`package`
    }

    fun dependencies(scope: DependenciesScope): Set<String> {
        val result = mutableSetOf<String>()

        // Hack-in some hardcoded dependencies
        when (name) {
            // services.core has framework classes injected
            "services.core" -> { result.add("framework") }
        }

        // Standard libraries
        if (scope == DependenciesScope.All) {
            if (type in ANDROID_TYPES) {
                result.add("framework")
                result.add("framework-res")
                if (members["no_standard_libs"] as? Boolean != true) {
                    result.add("core-all")
                }
                result.add("kotlin-stdlib")
            } else if (type in JAVA_TYPES) {
                if (members["no_standard_libs"] as? Boolean != true) {
                    result.add("core-all")
                }
                result.add("kotlin-stdlib")
            } else if (type == BlueprintType.AidlInterface || type == BlueprintType.JavaSdk) {
                result.add("framework")
                result.add("core-all")
            }
        }

        // Dynamic libraries
        if (scope != DependenciesScope.Static) {
            members["libs"]?.toStringCollection(result)
        }

        if (scope != DependenciesScope.Dynamic) {
            members["static_libs"]?.toStringCollection(result)
        }

        return result
    }

    fun defaults(): Set<String> {
        val result = mutableSetOf<String>()
        members["defaults"]?.toStringCollection(result)
        return result
    }

    fun outputJars(rootPath: OutputPaths): List<File> = when (type) {
        BlueprintType.AndroidLibrary -> {
            if (!isFromKati) {
                var path = rootPath.getPath("$relativePath_/$name/android_common/turbine-combined/$name.jar")
                if (!path.exists()) {
                    path = rootPath.getPath("$relativePath_/$name/android_common/combined/$name.jar")
                }
                listOf(path)
            } else {
                listOf(
                    rootPath.getPath("target/common/obj/JAVA_LIBRARIES/${name}_intermediates/classes.jar")
                )
            }
        }
        BlueprintType.JavaLibrary, BlueprintType.JavaLibraryStatic, BlueprintType.JavaLibraryHost,
        BlueprintType.JavaSdk, BlueprintType.SyspropLibrary -> {
            if (!isFromKati) {
                var path = rootPath.getPath("$relativePath_/$name/android_common/combined/$name.jar")
                if (!path.exists()) {
                    path = rootPath.getPath("$relativePath_/$name/android_common/turbine-combined/$name.jar")
                }
            listOf(path)
            } else {
                listOf(
                    rootPath.getPath("target/common/obj/JAVA_LIBRARIES/${name}_intermediates/classes.jar")
                )
            }
        }
        BlueprintType.JavaImport, BlueprintType.JavaImportHost -> {
            members["jars"]?.toStringList()?.map { File(path, it) } ?: emptyList()
        }
        BlueprintType.AidlInterface -> listOf(rootPath.getPath("$relativePath_/$name-java/android_common/turbine-combined/$name-java.jar"))
        BlueprintType.HidlInterface -> {
            val parts = name.split('@')
            if (parts.size != 2) {
                emptyList()
            } else {
                listOf(rootPath.getPath("$relativePath_/${parts[0]}-V${parts[1]}-java/android_common/turbine-combined/${parts[0]}-V${parts[1]}-java.jar"))
            }
        }
        else -> emptyList()
    }

    fun sources(relative: Boolean = true): List<String> {
        return members["srcs"]?.toSourcePaths(if (relative) relativePath_ else path.absolutePath) ?: emptyList()
    }

    fun generatedSources(rootPath: OutputPaths): List<File> = when (type) {
        BlueprintType.AidlInterface -> listOf(rootPath.getPath("$relativePath_/$name-java-source/gen/"))
        BlueprintType.HidlInterface -> {
            val parts = name.split('@')
            if (parts.size != 2) {
                emptyList()
            } else {
                listOf(rootPath.getPath("$relativePath_/${parts[0]}-V${parts[1]}-java_gen_java/gen/srcs/"))
            }
        }
        BlueprintType.SyspropLibrary -> {
            val path = rootPath.getPath("$relativePath_/$name/android_common/javac/$name.jar")
            if (path.exists()) {
                listOf(path)
            } else {
                val path = "$relativePath_/$name/android_common/gen/sysprop/$relativePath_/"
                members["api_packages"]?.toStringList()?.map {
                    rootPath.getPath("$path/$it.srcjar")
                }?.toList() ?: emptyList()
            }
        }
        else -> emptyList()
    }

    fun resources(relative: Boolean = true): List<String> = when (type) {
        BlueprintType.AndroidApp, BlueprintType.AndroidLibrary -> (members["resource_dirs"]?.toStringList() ?: listOf("res")).toSourcePaths(if (relative) relativePath_ else path.absolutePath) ?: emptyList()
        else -> emptyList()
    }

    fun generatedResources(): List<String> = when (type) {
        BlueprintType.AndroidImport -> listOf("$relativePath_/$name/android_common/aar/")
        else -> emptyList()
    }

    fun assets(relative: Boolean = true): List<String> = when (type) {
        BlueprintType.AndroidApp, BlueprintType.AndroidLibrary -> members["asset_dirs"]?.toSourcePaths(if (relative) relativePath_ else path.absolutePath) ?: emptyList()
        else -> emptyList()
    }

    fun aidl_includes_local(): List<String> = when (type) {
        BlueprintType.AndroidApp -> (members["aidl"] as? Map<*, *>)?.get("local_include_dirs")?.toStringList() ?: emptyList()
        else -> emptyList()
    }

    fun aidl_includes_global(): List<String> = when (type) {
        BlueprintType.AndroidApp, BlueprintType.AndroidLibrary, BlueprintType.JavaLibraryStatic -> (members["aidl"] as? Map<*, *>)?.get("include_dirs")?.toStringList() ?: emptyList()
        else -> emptyList()
    }

    companion object {
        const val DEFAULT_NAME = "Android.bp"
        const val DEFAULT_EXTENSION = "bp"

        fun create(type: String, members: Map<String, Any>, path: File, root: File): Blueprint? {
            val name = members["name"] as? String ?: return null
            val relativePath = FileUtil.getRelativePath(root, path) ?: return null

            return Blueprint(name, type, members, path, relativePath, false)
        }
    }
}

private fun Any.toStringCollection(target: MutableCollection<String>) {
    (this as? List<*>)?.mapNotNull { it as? String }?.toCollection(target)
}

private fun Any.toStringList() = (this as? List<*>)?.mapNotNull { it as? String }?.toList()

private fun Any.toSourcePaths(path: String? = null) = (this as? List<*>)?.mapNotNull { it as? String }?.map { item ->
    if (item[0] == ':') {
        item
    } else {
        val pos = item.indexOf('*')
        if (pos < 0) {
            val sourcePath = if (item.endsWith(".java") || item.endsWith(".kt")) {
                val end = item.lastIndexOf('/')
                if (end > 0) {
                    item.substring(0, end)
                } else {
                    item
                }
            } else {
                item
            }
            path?.let { "$it/$sourcePath" } ?: sourcePath
        } else {
            val end = item.lastIndexOf('/', pos)
            if (end < 0) {
                item.substring(0, pos)
            } else {
                item.substring(0, end)
            }.let { sourcePath ->
                path?.let {
                    "$path/$sourcePath"
                } ?: sourcePath
            }
        }
    }
}

class BlueprintElementType(debugName: String) : IElementType(debugName, BlueprintLanguage.INSTANCE)

class BlueprintTokenType(debugName: String) : IElementType(debugName, BlueprintLanguage.INSTANCE) {
    override fun toString(): String = "BlueprintToken.${super.toString()}"
}
