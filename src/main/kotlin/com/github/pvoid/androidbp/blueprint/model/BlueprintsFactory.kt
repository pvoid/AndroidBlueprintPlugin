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
        "java_library", "java_library_host", "java_library_static" -> createJavaLibrary(values)
        "java_import" -> createJavaImport(values, path)
        "java_sdk_library" -> createJavaSdkLibrary(values)
        "sysprop_library" -> createSyspropLibrary(values)
        "aidl_interface" -> createAidlInterface(values)
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

        val libs = mutableListOf("framework", "framework-res")
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

        val libs = mutableListOf("framework", "framework-res")
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
        val libs = mutableListOf("framework")
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

    private fun createJavaLibrary(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        val sources = (values["srcs"] as? List<*>)?.let { pattern ->
            pattern.mapNotNull { it as? String }.map { SourceSet(it) }
        } ?: emptyList()
        val libs = mutableListOf<String>()
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
        val libs = mutableListOf("framework")
        (values["static_libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        (values["libs"] as? List<*>)?.mapNotNull { it as? String }?.forEach {
            libs.add(it)
        }
        val defaults = (values["defaults"] as? List<*>)?.mapNotNull { it as? String  } ?: emptyList()
        return JavaSdkLibraryBlueprint(name, sources, libs, defaults)
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
        val libs = mutableListOf("framework")
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

    private fun createUnsupportedBlueprint(values: Map<String, Any>): Blueprint? {
        val name = values["name"] as? String ?: return null
        return UnsupportedBlueprint(name)
    }
}