/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sync

import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.github.pvoid.androidbp.idea.project.BlueprintAndroidModel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.io.File

internal class OnChangeSyncTask(
    private val project: Project,
    private val aospRoot: File,
    private val listener: (ProjectSystemSyncManager.SyncResult) -> Unit
) : BaseProjectSyncTask(project, "Initial project sync") {
    override fun run(indicator: ProgressIndicator) {
        indicator.text = "Updating project blueprints..."
        indicator.isIndeterminate = true
        updateJavaDependencies(aospRoot)
        updateProjectBlueprints()

        val facets = updateProjectFacets()
        indicator.text = "Updating source roots..."
        indicator.isIndeterminate = true
        updateSourceRoots()

        facets.forEach { facet ->
            BlueprintAndroidModel.register(facet)
            StudioResourceRepositoryManager.getInstance(facet).resetAllCaches()
        }

        listener(ProjectSystemSyncManager.SyncResult.SUCCESS)
    }
}