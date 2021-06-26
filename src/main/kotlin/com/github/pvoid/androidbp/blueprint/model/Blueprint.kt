/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.model

import java.io.File

interface Blueprint {
    val name: String
}

interface BlueprintWithSources : Blueprint {
    val sources: List<SourceSet>
}

interface BlueprintWithDynamicSources : Blueprint {
    fun getSources(basePath: File): File
}

interface BlueprintWithResources : Blueprint {
    val resources: List<SourceSet>
    fun getR(basePath: File): File
    fun getResApk(basePath: File): File
}

interface BlueprintWithDynamicResources : Blueprint {
    fun getResources(basePath: File): File
    fun getR(basePath: File): File
    fun getResApk(basePath: File): File
}

interface BlueprintWithAssets : Blueprint {
    val assets: List<SourceSet>
}

interface BlueprintWithAidls : Blueprint {
    val aidls: List<SourceSet>
}

interface BlueprintWithManifest : Blueprint {
    val manifest: String
}

interface BlueprintWithDynamicManifest : Blueprint {
    fun getManifest(basePath: File): File
}

interface BlueprintWithDependencies : Blueprint {
    val dependencies: List<String>
}

interface BlueprintWithDefaults : Blueprint {
    val defaults: List<String>
}

interface BlueprintWithArtifacts : Blueprint {
    fun getArtifacts(basePath: File): List<File>
}

class AndroidAppBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
    override val resources: List<SourceSet>,
    override val assets: List<SourceSet>,
    override val dependencies: List<String>,
    override val manifest: String,
    val privileged: Boolean,
    override val defaults: List<String>
) : Blueprint, BlueprintWithSources, BlueprintWithDependencies, BlueprintWithDefaults, BlueprintWithResources, BlueprintWithAssets, BlueprintWithManifest {

    override fun getR(basePath: File): File = File(basePath, "android_common/R.txt")
    override fun getResApk(basePath: File): File = File(basePath, "android_common/package-res.apk")
}

class AndroidLibraryBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
    override val resources: List<SourceSet>,
    override val assets: List<SourceSet>,
    override val dependencies: List<String>,
    override val manifest: String,
    override val defaults: List<String>
) : Blueprint, BlueprintWithSources, BlueprintWithDependencies, BlueprintWithDefaults, BlueprintWithResources, BlueprintWithAssets, BlueprintWithManifest, BlueprintWithArtifacts {
    override fun getArtifacts(basePath: File): List<File> = listOf(File(basePath, "android_common/combined/$name.jar"))
    override fun getR(basePath: File): File = File(basePath, "android_common/R.txt")
    override fun getResApk(basePath: File): File = File(basePath, "android_common/package-res.apk")
}

class AndroidLibraryImportBlueprint(
    override val name: String,
    override val dependencies: List<String>,
) : Blueprint, BlueprintWithDependencies, BlueprintWithDynamicResources, BlueprintWithArtifacts, BlueprintWithDynamicManifest {
    override fun getArtifacts(basePath: File): List<File> = listOf(File(basePath, "android_common/aar/classes.jar"))
    override fun getR(basePath: File): File = File(basePath, "android_common/aar/R.txt")
    override fun getResApk(basePath: File): File = File(basePath, "android_common/package-res.apk")
    override fun getResources(basePath: File): File = File(basePath, "android_common/aar/res/")
    override fun getManifest(basePath: File): File = File(basePath, "android_common/aar/AndroidManifest.xml")
}

class JavaDefaultBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
) : Blueprint, BlueprintWithSources

class FileGroupBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
) : Blueprint, BlueprintWithSources

class JavaBinaryBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
    override val dependencies: List<String>,
    override val defaults: List<String>
) : Blueprint, BlueprintWithSources, BlueprintWithDependencies, BlueprintWithDefaults

class JavaLibraryBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
    override val dependencies: List<String>,
    override val defaults: List<String>
) : Blueprint, BlueprintWithSources, BlueprintWithDependencies, BlueprintWithDefaults, BlueprintWithArtifacts {
    override fun getArtifacts(basePath: File): List<File> {
        var file = File(basePath, "android_common/combined/$name.jar")
        if (!file.exists()) {
            file = File(basePath, "android_common/turbine-combined/$name.jar")
        }
        return listOf(file)
    }
}

class JavaImportBlueprint(
    override val name: String,
    val libraries: List<File>
) : Blueprint, BlueprintWithArtifacts {
    override fun getArtifacts(basePath: File): List<File> = libraries
}

class JavaSdkLibraryBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
    override val dependencies: List<String>,
    override val defaults: List<String>
) : Blueprint, BlueprintWithSources, BlueprintWithDependencies, BlueprintWithDefaults, BlueprintWithArtifacts {
    override fun getArtifacts(basePath: File): List<File> = listOf(File(basePath, "android_common/turbine-combined/$name.jar"))
}

class SyspropLibraryBlueprint(
    override val name: String,
    override val sources: List<SourceSet>,
) : Blueprint, BlueprintWithSources, BlueprintWithArtifacts {
    override fun getArtifacts(basePath: File): List<File> = listOf(File(basePath, "android_common/turbine-combined/$name.jar"))
}

class AidlJavaInterfaceBlueprint(
    override val name: String,
    override val aidls: List<SourceSet>,
    override val dependencies: List<String>,
) : Blueprint, BlueprintWithAidls, BlueprintWithDependencies, BlueprintWithArtifacts, BlueprintWithDynamicSources {
    override fun getArtifacts(basePath: File): List<File> {
        val outPath = "${basePath.absolutePath}-java/"
        return listOf(File(outPath, "android_common/turbine-combined/$name-java.jar"))
    }

    override fun getSources(basePath: File): File {
        val outPath = "${basePath.absolutePath}-java-source/"
        return File(outPath, "gen")
    }
}

class UnsupportedBlueprint(
    override val name: String
) : Blueprint