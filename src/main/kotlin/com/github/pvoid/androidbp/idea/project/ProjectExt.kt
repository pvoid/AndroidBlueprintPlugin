/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.util.toIoFile
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.Makefile
import com.github.pvoid.androidbp.idea.project.sdk.AospSdkType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File

fun Project.guessAospRoot(): File? = generateSequence(guessProjectDir()?.toIoFile()) {
        it.parentFile
    }.firstOrNull {
        looksLikeAospRoot(it)
    }

fun Project.guessPlatformVersion(): Int? {
    val path = guessAospRoot() ?: return null
    return AospSdkType.parsePlatformVersion(path.absolutePath)
}

fun Project.getProjectBlueprint(): File? {
    val path = guessProjectDir() ?: return null
    val blueprintFile = File(path.toIoFile(), Blueprint.DEFAULT_NAME)
    return if (blueprintFile.exists()) {
        blueprintFile
    } else {
        null
    }
}

fun Project.getProjectMakefile(): File? {
    val path = guessProjectDir() ?: return null
    val makefile = File(path.toIoFile(), Makefile.DEFAULT_NAME)
    return if (makefile.exists()) {
        makefile
    } else {
        null
    }
}

/**
 * Checks if path looks like AOSP root path
 * @param path directory to check
 */
private fun looksLikeAospRoot(path: File): Boolean {
    var hasRepoFolder = false
    var hasDeviceFolder = false
    var hasVendorFolder = false
    var hasSdkFolder = false

    path.listFiles()?.forEach {
        if (it.name == ".repo") {
            hasRepoFolder = File(path, ".repo/manifest.xml").exists()
        }

        if (it.name == "device") {
            hasDeviceFolder = true;
        }

        if (it.name == "vendor") {
            hasVendorFolder = true;
        }

        if (it.name == "prebuilts") {
            hasSdkFolder = File(path, "prebuilts/sdk/tools/").exists()
        }
    }

    return hasRepoFolder && hasSdkFolder && (hasVendorFolder || hasDeviceFolder)
}
