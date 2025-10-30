/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.projectsystem.IdeaSourceProvider
import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.util.toVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class BlueprintSourceProvider(
    override val name: String,
    override val scopeType: ScopeType,
    private val moduleSystem: BlueprintModuleSystem,
    private val manifest: VirtualFile,
) : NamedIdeaSourceProvider {

    override val aidlDirectories: Iterable<VirtualFile> get() {
        return moduleSystem.blueprints.filter {
            it.isAndroidProject() || it.isJavaProject()
        }.flatMap { blueprint ->
            blueprint.aidl_includes_local().map {
                File(blueprint.path, it)
            }
        }.mapNotNull {
            it.toVirtualFile()
        }
    }
    override val aidlDirectoryUrls: Iterable<String>
        get() = aidlDirectories.map { it.url }

    override val assetsDirectories: Iterable<VirtualFile> get() {
        return moduleSystem.blueprints.flatMap {
            it.assets()
        }.mapNotNull {
            it.toVirtualFile()
        }
    }
    override val assetsDirectoryUrls: Iterable<String>
        get() = assetsDirectories.map { it.url }

    override val custom: Map<String, IdeaSourceProvider.Custom> = emptyMap()

    override val javaDirectories: Iterable<VirtualFile> get() {
        return moduleSystem.blueprints.filter {
            it.isAndroidProject() || it.isJavaProject()
        }.flatMap {
            it.sources()
        }.mapNotNull {
            it.toVirtualFile()
        }
    }
    override val javaDirectoryUrls: Iterable<String>
        get() = javaDirectories.map { it.url }

    override val jniLibsDirectories: Iterable<VirtualFile> = emptyList() // TODO:
    override val jniLibsDirectoryUrls: Iterable<String>
        get() = jniLibsDirectories.map { it.url }

    override val kotlinDirectories: Iterable<VirtualFile> = emptyList()
    override val kotlinDirectoryUrls: Iterable<String> = emptyList()

    override val manifestDirectories: Iterable<VirtualFile> = emptyList()
    override val manifestDirectoryUrls: Iterable<String> = emptyList()

    override val manifestFiles: Iterable<VirtualFile>
        get() = listOf(manifest)

    override val manifestFileUrls: Iterable<String>
        get() = manifestFiles.map { it.url }

    override val mlModelsDirectories: Iterable<VirtualFile> = emptyList()
    override val mlModelsDirectoryUrls: Iterable<String> = emptyList()

    override val renderscriptDirectories: Iterable<VirtualFile> = emptyList()
    override val renderscriptDirectoryUrls: Iterable<String> = emptyList()

    override val resDirectories: Iterable<VirtualFile> get() {
        return moduleSystem.blueprints.flatMap {
            it.resources()
        }.mapNotNull {
            it.toVirtualFile()
        }
    }
    override val resDirectoryUrls: Iterable<String>
        get() = resDirectories.map { it.url }

    override val resourcesDirectories: Iterable<VirtualFile> = emptyList()
    override val resourcesDirectoryUrls: Iterable<String> = emptyList()

    override val shadersDirectories: Iterable<VirtualFile> = emptyList()
    override val shadersDirectoryUrls: Iterable<String> = emptyList()

    override val baselineProfileDirectories: Iterable<VirtualFile> = emptyList()
    override val baselineProfileDirectoryUrls: Iterable<String> = emptyList()
}
