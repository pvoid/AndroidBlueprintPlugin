/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.builder.model.AaptOptions.Namespacing
import com.android.projectmodel.DynamicResourceValue
import com.android.sdklib.AndroidVersion
import com.android.tools.idea.gradle.project.build.PostProjectBuildTasksExecutor
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.ClassJarProvider
import com.android.tools.lint.detector.api.Desugaring
import com.github.pvoid.androidbp.blueprint.model.Blueprint
import com.github.pvoid.androidbp.blueprint.model.BlueprintWithManifest
import com.github.pvoid.androidbp.module.sdk.AospSdkData
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
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
            getManifest()?.`package`?.value ?: ""
        }
    }

    override fun getAllApplicationIds(): Set<String> = setOf(applicationId)

    override fun overridesManifestPackage(): Boolean = false

    override fun isDebuggable(): Boolean = true

    override fun getMinSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getRuntimeMinSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getTargetSdkVersion(): AndroidVersion = AndroidVersion(getPlatformVersion(), null)

    override fun getVersionCode(): Int = 1 // TODO: Real version number

    override fun isGenerated(file: VirtualFile): Boolean = false

    override fun getClassJarProvider(): ClassJarProvider = mProjectJarsProvider

    override fun isClassFileOutOfDate(module: Module, fqcn: String, classFile: VirtualFile): Boolean
        = testIsClassFileOutOfDate(module, fqcn, classFile)

    override fun getNamespacing(): Namespacing
        = Namespacing.DISABLED

    override fun getDesugaring(): Set<Desugaring> = Desugaring.NONE

    override fun getResValues(): MutableMap<String, DynamicResourceValue> {
        return mResources
    }

    private fun testIsClassFileOutOfDate(module: Module, fqcn: String, classFile: VirtualFile): Boolean {
        val project = module.project
        val scope = module.moduleWithDependenciesScope
        val sourceFile = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
            JavaPsiFacade.getInstance(project).findClass(fqcn, scope)?.let { psiClass ->
                psiClass.containingFile?.virtualFile
            }
        }?: return false

        // Edited but not yet saved?
        if (FileDocumentManager.getInstance().isFileModified(sourceFile)) {
            return true
        }

        // Check timestamp
        val sourceFileModified = sourceFile.timeStamp

        // User modifications on the source file might not always result on a new .class file.
        // We use the project modification time instead to display the warning more reliably.
        var lastBuildTimestamp = classFile.timeStamp
        val projectBuildTimestamp = PostProjectBuildTasksExecutor.getInstance(project).lastBuildTimestamp
        if (projectBuildTimestamp != null) {
            lastBuildTimestamp = projectBuildTimestamp
        }
        return lastBuildTimestamp in 1 until sourceFileModified
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