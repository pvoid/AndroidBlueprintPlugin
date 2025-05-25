/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp

import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.getSyncManager
import com.android.tools.idea.util.toIoFile
import com.github.pvoid.androidbp.blueprint.Blueprint
import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class BlueprintFsListener(
    private val project: Project
) : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        val files = events.filter { it.file?.name == Blueprint.DEFAULT_NAME }.mapNotNull { it as? VFileContentChangeEvent }.map {
            it.file
        }.toList()

        if (files.isNotEmpty()) {
            project.getSyncManager().requestSyncProject(ProjectSystemSyncManager.SyncReason.PROJECT_MODIFIED)

            val table = BlueprintsTable.getInstance(project)
            files.asSequence().map { it.toIoFile() }.forEach(table::update)
        }
    }
}