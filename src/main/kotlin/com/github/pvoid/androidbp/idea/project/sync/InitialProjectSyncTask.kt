/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sync

import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.res.ResourceRepositoryManager
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.sdk.wizard.SdkQuickfixUtils
import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.github.pvoid.androidbp.idea.project.deprecated.BlueprintAndroidModel
import com.github.pvoid.androidbp.idea.project.guessPlatformVersion
import com.intellij.diagnostic.PluginException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.android.sdk.AndroidSdkAdditionalData
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.io.File

internal class InitialProjectSyncTask (
    private val project: Project,
    private val aospRoot: File,
    private val listener: (ProjectSystemSyncManager.SyncResult) -> Unit
) : BaseProjectSyncTask(project, "Initial project sync") {

    override fun run(indicator: ProgressIndicator) {

        indicator.text = "Setting up JDK..."
        indicator.isIndeterminate = true
        if (!setUpJdk()) {
            listener(ProjectSystemSyncManager.SyncResult.SKIPPED)
            return
        }

        indicator.text = "Collecting blueprint files..."
        indicator.isIndeterminate = true
        val blueprintFiles = collectBlueprintFiles(aospRoot)

        indicator.text = "Parsing blueprints..."
        val blueprints = parseBlueprints(indicator, aospRoot, blueprintFiles)

        BlueprintsTable.getInstance(project).update(blueprintFiles, blueprints)

        indicator.text = "Updating project blueprints..."
        indicator.isIndeterminate = true
        updateProjectBlueprints()
        updateSourceRoots()

        indicator.text = "Updating project dependencies..."
        indicator.isIndeterminate = true
        updateJavaDependencies(aospRoot)
        updateAndroidDependencies(aospRoot)
        updateProjectFacets().forEach { facet ->
            BlueprintAndroidModel.register(facet)
//            fixLayoutLibrary(facet)
            ResourceRepositoryManager.getInstance(facet).resetAllCaches()
        }

        listener(ProjectSystemSyncManager.SyncResult.SUCCESS)
    }

    private fun setUpJdk(): Boolean {
        val jdkPath = File(aospRoot.absolutePath, "prebuilts/jdk/jdk8/linux-x86")
        val manager = ProjectRootManager.getInstance(project)
        var addSdk = false

        if (!AndroidSdkUtils.isAndroidSdkAvailable()) {
            showSdkConfigurationNotification()
            return false
        }
        val androidSdk = AndroidSdks.getInstance().allAndroidSdks.firstOrNull() ?: return false

        var sdk = ProjectJdkTable.getInstance().allJdks.firstOrNull { it.homePath == jdkPath.absolutePath }
        if (sdk == null) {
            sdk = JavaSdk.getInstance().createJdk("AOSP JDK (API ${project.guessPlatformVersion()})", jdkPath.absolutePath, false)
            addSdk = true
        }

        return WriteAction.computeAndWait<Boolean, Throwable> {
            if (addSdk) {
                ProjectJdkTable.getInstance().addJdk(sdk)
            }
            manager.projectSdk = sdk

            sdk.sdkModificator.apply {
                this.sdkAdditionalData = AndroidSdkAdditionalData(androidSdk, sdk)
                commitChanges()
            }

            true
        }
    }

    private fun showSdkConfigurationNotification() {
        NotificationGroupManager.getInstance().getNotificationGroup("AOSP Blueprint").createNotification(
            "AOSP Blue Plugin",
            "<b>Android SDK</b> path is not configured. Android SDK is required to generate layouts and drawable previews. Please configure one before using the plugin",
            NotificationType.ERROR
        ).addAction(object : NotificationAction("Configure SDK") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                try {
                    SdkQuickfixUtils.showAndroidSdkManager()
                } catch (e: PluginException) {
                    // Swallow deprecated exception. Not under our control
                }
            }
        }).notify(project)
    }

    private fun showInvalidBlueprintLocation() {
        NotificationGroupManager.getInstance().getNotificationGroup("AOSP Blueprint").createNotification(
            "AOSP Blue Plugin",
            "Blueprint parent folder doesn't look like AOSP source code",
            NotificationType.WARNING
        ).notify(project)
    }

    /*
    private fun fixLayoutLibrary(facet: AndroidFacet) {
        val data = AndroidSdkData.getSdkData(facet) ?: return
        data.targets.forEach { target ->
            try {
                val compatTarget = StudioEmbeddedRenderTarget.getCompatibilityTarget(target)
                data.getTargetData(compatTarget).getLayoutLibrary(project)
            } catch (e: UnsatisfiedLinkError) {
                LOG.error("Can't load layout library", e)
            } catch (e: Throwable) {
                LOG.error("Can't load layout library", e)
            }
        }
    }
 */
}