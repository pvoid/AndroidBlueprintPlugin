/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.github.pvoid.androidbp.LOG
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.AospProjectHelper
import com.github.pvoid.androidbp.module.sdk.aospSdkData
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.intellij.facet.FacetManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.ui.AppUIUtil
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.util.concurrent.Executors

private val SYNC_EXECUTOR = Executors.newSingleThreadExecutor()

class AospProjectSystemSyncManager(
    private val mProject: Project
) : ProjectSystemSyncManager, BlueprintsProvider {

    @Volatile
    private var mSyncRequired: Boolean = true

    private var mBlueprints: List<Blueprint> = emptyList()

    private var mWatchedFiles = listOf<String>()

    override val blueprints: List<Blueprint>
        get() = mBlueprints

    fun setSyncRequired() {
        mSyncRequired = true
    }

    fun isFileWatched(url: String) = mWatchedFiles.contains(url)

    override fun syncProject(reason: ProjectSystemSyncManager.SyncReason): ListenableFuture<ProjectSystemSyncManager.SyncResult> {
        val future = SettableFuture.create<ProjectSystemSyncManager.SyncResult>()
        val syncTask = object : Task.ConditionalModal(null, "Indexing blueprints", false, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                future.set(
                    try {
                        doSync()
                        ProjectSystemSyncManager.SyncResult.SUCCESS
                    } catch (e: Exception) {
                        LOG.error("Project sync failed", e)
                        ProjectSystemSyncManager.SyncResult.FAILURE
                    }
                )
            }
        }
        ProgressManager.getInstance().run(syncTask)
        return future
    }

    override fun isSyncInProgress() = false

    override fun isSyncNeeded() = mSyncRequired

    override fun getLastSyncResult() = ProjectSystemSyncManager.SyncResult.SUCCESS

    private fun doSync() {
        val watchedFiles = mutableListOf<String>()

        mBlueprints = AospProjectHelper.blueprintFileForProject(mProject)?.let {
            watchedFiles.add(it.url)
            BlueprintsTable.get(it)
        }?.filter(AospProjectHelper::shouldHaveFacet) ?: emptyList()

        val sdk = ProjectRootManager.getInstance(mProject).projectSdk

        if (sdk == null || mBlueprints.isEmpty()) {
            LOG.error("doSync() skipped for ${mProject.name}")

            AppUIUtil.invokeLaterIfProjectAlive(mProject) {
                mProject.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
                    .syncEnded(ProjectSystemSyncManager.SyncResult.SKIPPED_OUT_OF_DATE)
            }
            return
        }

        val libs = AospProjectHelper.createDependencies(mProject, sdk)
        AospProjectHelper.addDependencies(mProject, libs)

        libs.mapNotNull { it.name }
            .mapNotNull { sdk.aospSdkData?.getBlueprintFile(it) }
            .forEach {
                watchedFiles.add(it.url)
            }

        // Adding aidls and sources to source roots
        AospProjectHelper.updateSourceRoots(mProject, mBlueprints)

        mSyncRequired = false
        AppUIUtil.invokeLaterIfProjectAlive(mProject) {
            mProject.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
                .syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
        }

        mWatchedFiles = watchedFiles
    }
}