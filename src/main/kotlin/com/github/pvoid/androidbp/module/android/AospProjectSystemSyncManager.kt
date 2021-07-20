/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.util.toIoFile
import com.github.pvoid.androidbp.LOG
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.AospProjectHelper
import com.github.pvoid.androidbp.module.sdk.aospSdkData
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.facet.FacetManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.AppUIUtil
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File
import java.util.concurrent.Executors

private val SYNC_EXECUTOR = Executors.newSingleThreadExecutor()

class AospProjectSystemSyncManager(
    private val mProject: Project
) : ProjectSystemSyncManager {

    @Volatile
    private var mSyncRequired: Boolean = true

    private val mPublisher = mProject.messageBus.syncPublisher(FacetManager.FACETS_TOPIC)

    private val mExecutors = MoreExecutors.listeningDecorator(SYNC_EXECUTOR)

    private var mBlueprints: List<Blueprint> = emptyList()

    private var mWatchedFiles = listOf<String>()

    val blueprints: List<Blueprint>
        get() = mBlueprints

    fun setSyncRequired() {
        mSyncRequired = true
    }

    fun isFileWatched(url: String) = mWatchedFiles.contains(url)

    override fun syncProject(reason: ProjectSystemSyncManager.SyncReason): ListenableFuture<ProjectSystemSyncManager.SyncResult> {
        return mExecutors.submit<ProjectSystemSyncManager.SyncResult> {
            try {
                doSync()
                ProjectSystemSyncManager.SyncResult.SUCCESS
            } catch (e: Exception) {
                LOG.error("Project sync failed", e)
                ProjectSystemSyncManager.SyncResult.FAILURE
            }
        }
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

        val facets = FacetManager.getInstance(mProject.allModules().first())
        val sdk = ProjectRootManager.getInstance(mProject).projectSdk

        if (sdk != null) {
            val libs = AospProjectHelper.createDependencies(mProject, sdk)
            AospProjectHelper.addDependencies(mProject, libs)

            libs.mapNotNull { it.name }
                .mapNotNull { sdk.aospSdkData?.getBlueprintFile(it) }
                .forEach {
                    watchedFiles.add(it.url)
                }

            // Updating facets
            mBlueprints.forEach { blueprint ->
                val facet = facets.findFacet(AndroidFacet.ID, blueprint.name) ?: return@forEach
                AospProjectHelper.updateFacet(sdk, blueprint, facet)
                mPublisher.facetConfigurationChanged(facet)
            }

            // Adding aidls and sources to source roots
            AospProjectHelper.updateSourceRoots(mProject, mBlueprints)
        }

        mSyncRequired = false
        AppUIUtil.invokeLaterIfProjectAlive(mProject) {
            mProject.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
                .syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
        }

        mWatchedFiles = watchedFiles
    }
}