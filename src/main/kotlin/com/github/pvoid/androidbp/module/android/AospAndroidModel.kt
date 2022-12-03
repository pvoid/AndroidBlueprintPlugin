/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.projectmodel.DynamicResourceValue
import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.idea.model.Namespacing
import com.android.tools.lint.detector.api.Desugaring
import com.github.pvoid.androidbp.blueprint.model.Blueprint
import com.github.pvoid.androidbp.blueprint.model.BlueprintWithManifest
import com.github.pvoid.androidbp.blueprint.model.JavaSdkLibraryBlueprint
import com.github.pvoid.androidbp.module.sdk.AospSdkData
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.android.util.AndroidUtils
import java.io.File

class AospAndroidModel(
    private val mProject: Project,
    private val mBlueprint: Blueprint,
) : AndroidModel {
    private val mProjectJarsProvider = AospClassJarProvider()

    private val mResources = mutableMapOf<String, DynamicResourceValue>()

    override fun getApplicationId(): String {
        return ReadAction.compute<String, Throwable> {
            if (mBlueprint is JavaSdkLibraryBlueprint) {
                mBlueprint.apiPackages.firstOrNull()
            } else {
                getManifest()?.`package`?.value
            } ?: "com.example"
        }
    }

    override fun getAllApplicationIds(): Set<String> = setOf(applicationId)

    override fun overridesManifestPackage(): Boolean = false

    override fun isDebuggable(): Boolean = true

    override fun getMinSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getRuntimeMinSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getTargetSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getNamespacing(): Namespacing
        = Namespacing.DISABLED

    override fun getDesugaring(): Set<Desugaring> = Desugaring.NONE

    override fun getResValues(): MutableMap<String, DynamicResourceValue> {
        return mResources
    }

    private fun getManifest(): Manifest? {
        return (mBlueprint as? BlueprintWithManifest)?.manifest?.let {
            VirtualFileManager.getInstance().findFileByUrl(File(mProject.basePath!!, it).toFileSystemUrl())
        }?.let {
            AndroidUtils.loadDomElement(mProject, it, Manifest::class.java)
        }
    }

    private fun getPlatformVersion(): Int {
        val data = ProjectRootManager.getInstance(mProject).projectSdk?.sdkAdditionalData as? AospSdkData
        return data?.platformVersion ?: 0
    }
}