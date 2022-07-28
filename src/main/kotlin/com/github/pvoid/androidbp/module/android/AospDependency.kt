/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.SdkConstants
import com.android.ide.common.util.PathString
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.projectmodel.RecursiveResourceFolder
import com.android.projectmodel.ResourceFolder
import com.android.tools.idea.AndroidPsiUtils
import com.android.tools.idea.util.toPathString
import com.github.pvoid.androidbp.blueprint.BlueprintHelper
import com.github.pvoid.androidbp.blueprint.model.Blueprint
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.xml.XmlFile
import com.intellij.serviceContainer.AlreadyDisposedException
import org.jetbrains.android.dom.manifest.AndroidManifestXmlFile

class AospDependency(
    private val mProject: Project,
    private val mBlueprint: Blueprint
) : ExternalAndroidLibrary {

    private val sdk: Sdk?
        get() = try {
            ProjectRootManager.getInstance(mProject).projectSdk
        } catch (e: AlreadyDisposedException) {
            null
        }

    override val address: String
        get() = mBlueprint.name

    override val hasResources: Boolean
        get() {
            val projectSdk = sdk ?: return false
            return BlueprintHelper.collectBlueprintResources(mBlueprint, projectSdk).isNotEmpty()
        }

    override val location: PathString? = null

    override val manifestFile: PathString?
        get() {
            val projectSdk = sdk ?: return null
            return BlueprintHelper.getBlueprintManifest(mBlueprint, projectSdk)?.toPathString()
        }

    override val packageName: String?
        get() {
            val projectSdk = sdk ?: return null
            val manifest = BlueprintHelper.getBlueprintManifest(mBlueprint, projectSdk) ?: return null

            val psiFile = AndroidPsiUtils.getPsiFileSafely(mProject, manifest)
            return (psiFile as? XmlFile)?.let {
                ReadAction.compute<String?, Throwable> {
                    if (it.rootTag?.name == SdkConstants.TAG_MANIFEST) {
                        AndroidManifestXmlFile(it).packageName
                    }
                     else {
                        null
                    }
                }
            }
        }

    override val resApkFile: PathString?
        get() {
            val projectSdk = sdk ?: return null
            return BlueprintHelper.getBlueprintResApk(mBlueprint, projectSdk)?.toPathString()
        }

    override val resFolder: ResourceFolder?
        get() {
            val projectSdk = sdk ?: return null
            val res = BlueprintHelper.collectBlueprintResources(mBlueprint, projectSdk)
            if (res.isEmpty()) {
                return null
            }
            return RecursiveResourceFolder(res.first().toPathString()) // TODO: Get folder with all resources
        }

    override val assetsFolder: PathString?
        get() {
            val projectSdk = sdk ?: return null
            val res = BlueprintHelper.collectBlueprintAssets(mBlueprint, projectSdk)
            if (res.isEmpty()) {
                return null
            }
            return res.first().toPathString()
        }

    override val symbolFile: PathString?
        get() {
            val projectSdk = sdk ?: return null
            return BlueprintHelper.getBlueprintR(mBlueprint, projectSdk)?.toPathString()
        }
}