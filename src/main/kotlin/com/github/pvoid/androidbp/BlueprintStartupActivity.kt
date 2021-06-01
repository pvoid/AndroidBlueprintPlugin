/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp

import com.github.pvoid.androidbp.module.AospProjectHelper
import com.github.pvoid.androidbp.module.sdk.AOSP_SDK_TYPE_NAME
import com.github.pvoid.androidbp.module.sdk.AospSdkHelper
import com.github.pvoid.androidbp.module.sdk.aospSdkData
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.startup.StartupActivity

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task


class BlueprintStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val blueprint = AospProjectHelper.blueprintFileForProject(project) ?: return

        if (ModuleUtil.findModuleForFile(blueprint, project) != null) {
            // Check if AOSP sdk applied
            val manager = ProjectRootManager.getInstance(project)
            val projectSdk = manager.projectSdk

            if (projectSdk != null && projectSdk.sdkType.name == AOSP_SDK_TYPE_NAME) {
                ProgressManager.getInstance().run( object : Task.Backgroundable(project, "Checking dependencies", false, ALWAYS_BACKGROUND) {
                    override fun run(indicator: ProgressIndicator) {
                        if (projectSdk.aospSdkData?.isOld() == true) {
                            AospSdkHelper.updateAdditionalData(projectSdk, indicator)
                        }

                        val libs = AospProjectHelper.createDependencies(project, projectSdk)
                        indicator.text = "Assigning dependencies"
                        AospProjectHelper.addDependencies(project, libs)
                        indicator.text = "Adding android sdk support"
                        AospProjectHelper.addSourceRoots(project, projectSdk)
                        AospProjectHelper.addFacets(project, projectSdk)
                    }
                } )
            } else {
                val notification =
                    NotificationGroupManager.getInstance().getNotificationGroup("Android Module Importing")
                        .createNotification(
                            "Android source code detected",
                            "Android.bp file was found. Initialize AOSP source code module?",
                            NotificationType.INFORMATION
                        )
                notification.addAction(object : NotificationAction("Configure...") {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        notification.hideBalloon()

                        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Configuring project", false, ALWAYS_BACKGROUND) {
                            override fun run(indicator: ProgressIndicator) {
                                indicator.text = "Assigning AOSP SDK"
                                AospProjectHelper.checkAndAssignSdk(project, indicator)?.let { sdk ->
                                    indicator.text = "Creating libraries"
                                    val libs = AospProjectHelper.createDependencies(project, sdk)
                                    indicator.text = "Assigning dependencies"
                                    AospProjectHelper.addDependencies(project, libs)
                                    indicator.text = "Adding android sdk support"
                                    AospProjectHelper.addSourceRoots(project, sdk)
                                    AospProjectHelper.addFacets(project, sdk)
                                }
                            }
                        })

                    }
                })
                notification.notify(project)
            }
        }
    }
}
