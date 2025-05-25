/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sync

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager.SyncResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.ui.AppUIUtil
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.io.File

class BlueprintSyncManager(
    private val project: Project,
    private val aospRoot: File
) : ProjectSystemSyncManager, ProjectJdkTable.Listener {

    private val mGuard = Any()

    @Volatile
    private var isSyncNeeded = true

    private var lastSyncResult = SyncResult.UNKNOWN

    private var currentSync: SettableFuture<SyncResult>? = null

    private var messageBusConnection: MessageBusConnection? = null

    override fun getLastSyncResult(): SyncResult {
        return synchronized(mGuard) {
            lastSyncResult
        }
    }

    override fun isSyncInProgress(): Boolean {
        return synchronized(mGuard) {
            currentSync != null
        }
    }

    override fun isSyncNeeded(): Boolean {
        return isSyncNeeded
    }

    override fun syncProject(reason: ProjectSystemSyncManager.SyncReason): ListenableFuture<SyncResult> {
        return requestSyncProject(reason)
    }

    override fun requestSyncProject(reason: ProjectSystemSyncManager.SyncReason): ListenableFuture<SyncResult> {
        return synchronized(mGuard) {
            var future = currentSync

            if (future == null) {
                future = SettableFuture.create<SyncResult>()

                val task = when (reason) {
                    ProjectSystemSyncManager.SyncReason.PROJECT_LOADED -> InitialProjectSyncTask(
                        project,
                        aospRoot,
                        this::onInitSyncResult
                    )
                    ProjectSystemSyncManager.SyncReason.PROJECT_MODIFIED -> if (lastSyncResult != SyncResult.SKIPPED && lastSyncResult != SyncResult.UNKNOWN) {
                        // Do OnChange sync only if the initial one finished successfully
                        OnChangeSyncTask(
                            project,
                            aospRoot,
                            this::onSyncResult
                        )
                    } else {
                        null
                    }
                    else -> null
                }

                if (task != null) {
                    ProgressManager.getInstance().run(task)
                } else {
                    future.set(SyncResult.SKIPPED)
                }

                currentSync = future
            }

            future!!
        }
    }

    private fun onInitSyncResult(result: SyncResult) {
        val currentState = synchronized(mGuard) {
            lastSyncResult
        }

        onSyncResult(result)

        synchronized(mGuard) {
            if (messageBusConnection == null && result == SyncResult.SKIPPED && currentState == SyncResult.UNKNOWN) {
                messageBusConnection = project.messageBus.connect().apply {
                    subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, this@BlueprintSyncManager)
                }
            } else if (messageBusConnection != null && result != SyncResult.SKIPPED) {
                messageBusConnection?.disconnect()
            }
        }
    }

    private fun onSyncResult(result: SyncResult) {
        synchronized(mGuard) {
            lastSyncResult = result
            currentSync?.set(result)
            currentSync = null
        }

        AppUIUtil.invokeLaterIfProjectAlive(project) {
            project.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC).syncEnded(result)
        }
    }

    override fun jdkAdded(jdk: Sdk) {
        if (AndroidSdkUtils.isAndroidSdkAvailable()) {
            val state = synchronized(mGuard) {
                lastSyncResult
            }

            if (state == SyncResult.SKIPPED) {
                syncProject(ProjectSystemSyncManager.SyncReason.PROJECT_LOADED)
            }

            messageBusConnection?.disconnect()
        }
    }
}