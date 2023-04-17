/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.AndroidProjectSystemProvider
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager.SyncReason
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File

// NOTE: Must stay the same in all cost
private const val PROJECT_SYSTEM_ID = "com.github.pvoid.androidbp.AospAndroidProjectSystem"

class BlueprintProjectSystemProvider(
    private val project: Project
) : AndroidProjectSystemProvider {

    private val aospRoot: File? = project.guessAospRoot()

    private val projectSystemInstance by lazy {
        if (aospRoot == null) {
            throw AssertionError("AOSP root was not found. The project is not applicable")
        }
        BlueprintProjectSystem(project, aospRoot)
    }

    override val id: String = PROJECT_SYSTEM_ID

    override val projectSystem: AndroidProjectSystem
        get() {
            projectSystemInstance.getSyncManager().syncProject(SyncReason.PROJECT_LOADED)
            return projectSystemInstance
        }

    override fun isApplicable(): Boolean {
        val root = project.guessProjectDir() ?: return false

        return aospRoot != null && root.children.asSequence().any {
            !it.isDirectory && it.isValid && it.name == Blueprint.DEFAULT_NAME
        }
    }
}
