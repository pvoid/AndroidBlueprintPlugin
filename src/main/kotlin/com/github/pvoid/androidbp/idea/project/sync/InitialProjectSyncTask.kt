/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.sync

import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.res.ResourceUpdateTraceSettings
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.sdk.wizard.SdkQuickfixUtils
import com.github.pvoid.androidbp.blueprint.BlueprintsTable
import com.github.pvoid.androidbp.idea.project.deprecated.BlueprintAndroidModel
import com.github.pvoid.androidbp.idea.project.guessPlatformVersion
import com.github.pvoid.androidbp.idea.project.sdk.AospSdkType
import com.intellij.diagnostic.PluginException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel
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

        indicator.text = "Collecting makefiles..."
        indicator.isIndeterminate = true
        val katiFiles = collectKatiFiles(aospRoot)

        indicator.text = "Parsing blueprints..."
        val blueprints = parseBlueprints(indicator, aospRoot, blueprintFiles)

        indicator.text = "Parsing makefiles..."
        val makefiles = parseMakefiles(indicator, aospRoot, katiFiles)

        BlueprintsTable.getInstance(project).update(blueprints + makefiles)

        indicator.text = "Updating project blueprints..."
        indicator.isIndeterminate = true
        updateProjectBlueprints()

        indicator.text = "Updating project dependencies..."
        indicator.isIndeterminate = true
        updateJavaDependencies(aospRoot)
        updateAndroidDependencies(aospRoot)
        val facets = updateProjectFacets()

        indicator.text = "Updating source roots..."
        indicator.isIndeterminate = true
        updateSourceRoots()

        facets.forEach { facet ->
            BlueprintAndroidModel.register(facet)
            StudioResourceRepositoryManager.getInstance(facet).resetAllCaches()
        }

        listener(ProjectSystemSyncManager.SyncResult.SUCCESS)

        ResourceUpdateTraceSettings.getInstance().enabled = true
    }

    private fun setUpJdk(): Boolean {
        val manager = ProjectRootManager.getInstance(project)

        if (!AndroidSdkUtils.isAndroidSdkAvailable()) {
            showSdkConfigurationNotification()
            return false
        }
        val platformVersion = project.guessPlatformVersion() ?: return false
        val androidSdk = AndroidSdks.getInstance().allAndroidSdks.first {
            (it.sdkAdditionalData as? AndroidSdkAdditionalData)?.androidPlatform?.apiLevel == platformVersion
        } ?: return false

        val jdksTable = ProjectJdkTable.getInstance()
        var addSdk = false
        var sdk = jdksTable.allJdks.firstOrNull { it.sdkType is AospSdkType && it.homePath == aospRoot.absolutePath }
        if (sdk == null) {
            sdk =  AospSdkType.getInstance().createJdk("AOSP JDK (API $platformVersion)", aospRoot.absolutePath, androidSdk)
            addSdk = true
        } else if (sdk.sdkAdditionalData == null) {
            AospSdkType.getInstance().updateAdditionalData(sdk, androidSdk)
        }

        return WriteAction.computeAndWait<Boolean, Throwable> {
            if (addSdk) {
                jdksTable.addJdk(sdk!!)
                jdksTable.saveOnDisk()
                sdk = jdksTable.allJdks.firstOrNull { it.sdkType is AospSdkType && it.homePath == aospRoot.absolutePath }
            }
            manager.projectSdk = sdk

            module?.apply {
                ModuleRootManager.getInstance(this).modifiableModel.apply{
                    getModuleExtension(
                        LanguageLevelModuleExtension::class.java
                    ).languageLevel = LanguageLevel.JDK_1_8
                    inheritSdk()
                    commit()
                }
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
}