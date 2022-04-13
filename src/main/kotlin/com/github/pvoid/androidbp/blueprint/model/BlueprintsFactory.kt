/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import java.io.File

class BlueprintsFactory {
    fun create(name: CharSequence, values: Map<String, Any>, path: File): Blueprint? = when (name) {
        "android_app" -> createAndroidApp(values)
        "android_library" -> createAndroidLibrary(values)
        "android_library_import" -> createAndroidLibraryImport(values)
        "java_defaults" -> createJavaDefault(values)
        "filegroup" -> createFileGroup(values)
        "java_library", "java_library_host", "java_library_static", "java_test_host" -> createJavaLibrary(values)
        "java_binary", "java_binary_host" -> createJavaBinary(values)
        "java_import" -> createJavaImport(values, path)
        "java_sdk_library" -> createJavaSdkLibrary(values)
        "sysprop_library" -> createSyspropLibrary(values)
        "aidl_interface" -> createAidlInterface(values)
        "hidl_interface" -> createHidlInterface(values)
        "java_genrule" -> createJavaGenRule(values)
        else -> createUnsupportedBlueprint(values)
    }

    private fun createAndroidApp(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val resources = (values["resource_dirs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: listOf(SourceSet("res"))
        val assets = (values["asset_dirs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()

        val libs = mutableListOf("framework", "framework-res", "core-all", "kotlin-stdlib")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val manifest = values["manifest"] as? String ?: "AndroidManifest.xml"
        val privileged = values["privileged"] == true
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()

        return AndroidAppBlueprint(name, sources, resources, assets, libs, manifest, privileged, defaults)
    }

    private fun createAndroidLibrary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val resources = (values["resource_dirs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: listOf(SourceSet("res"))
        val assets = (values["asset_dirs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()

        val libs = mutableListOf("framework", "framework-res", "core-all", "kotlin-stdlib")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val manifest = values["manifest"] as? String ?: "AndroidManifest.xml"
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()

        return AndroidLibraryBlueprint(name, sources, resources, assets, libs, manifest, defaults)
    }

    private fun createAndroidLibraryImport(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val libs = mutableListOf("framework", "core-all", "kotlin-stdlib")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        return AndroidLibraryImportBlueprint(name, libs)
    }

    private fun createJavaDefault(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        return JavaDefaultBlueprint(name, sources)
    }

    private fun createFileGroup(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        return FileGroupBlueprint(name, sources)
    }

    private fun createJavaBinary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val libs = mutableListOf<String>("core-all", "kotlin-stdlib")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()
        return JavaBinaryBlueprint(name, sources, libs, defaults)
    }

    private fun createJavaLibrary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val libs = mutableListOf<String>("core-all", "kotlin-stdlib")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()
        return JavaLibraryBlueprint(name, sources, libs, defaults)
    }

    private fun createJavaSdkLibrary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val libs = mutableListOf("framework", "core-all")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()
        val apiPackages = (values["api_packages"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()
        return JavaSdkLibraryBlueprint(name, sources, libs, defaults, apiPackages)
    }

    private fun createJavaImport(values: Map<String, Any>, path: File): Blueprint? {
        val name = values["name"] as? String ?: return null

        return JavaImportBlueprint(
            name = name,
            libraries = (values["jars"] as? List<*>)?.mapNotNull { it as? String }?.map { jar ->
                File(path, jar)
            } ?: emptyList()
        )
    }

    private fun createSyspropLibrary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        return SyspropLibraryBlueprint(name, sources)
    }

    private fun createAidlInterface(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val libs = mutableListOf("framework", "core-all")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }

        val backend = values["backend"] as? Map<*, *> ?: return null
        val javaBackend = backend["java"] as? Map<*, *> ?: return null

        if (javaBackend["enabled"] != true) {
            return null
        }

        return AidlJavaInterfaceBlueprint(name, sources, libs)
    }

    private fun createHidlInterface(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()

        val parts = name.split('@')
        if (parts.size != 2) {
            return null
        }

        val genJava = values["gen_java"] as? Boolean == true
        return HidlInterfaceLibrary(name, parts[0], parts[1], sources, genJava)
    }

    private fun createJavaGenRule(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val srcs = mutableListOf<SourceSet>()
        val dependencies = mutableListOf<String>()

        (values["srcs"] as? List<*>)?.filterIsInstance<String>()?.forEach { src ->
            if (src.startsWith(":")) {
                dependencies.add(src.substring(1))
            } else {
                srcs.add(SourceSet(src))
            }
        }
        return JavaGenRuleBlueprint(name, srcs, dependencies)
    }

    private fun createUnsupportedBlueprint(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        return UnsupportedBlueprint(name)
    }
}
