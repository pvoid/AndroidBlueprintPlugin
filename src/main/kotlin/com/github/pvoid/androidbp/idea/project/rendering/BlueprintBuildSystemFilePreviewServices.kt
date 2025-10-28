/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.rendering

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager
import com.android.tools.idea.projectsystem.ProjectSystemToken
import com.android.tools.idea.rendering.BuildTargetReference
import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices
import com.android.tools.idea.rendering.tokens.BuildSystemFilePreviewServices.BuildTargets
import com.android.tools.idea.run.deployment.liveedit.tokens.ApplicationLiveEditServices
import com.github.pvoid.androidbp.idea.project.BlueprintModuleClassFinder
import com.github.pvoid.androidbp.idea.project.BlueprintProjectSystem
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface BlueprintToken : ProjectSystemToken {
    override fun isApplicable(projectSystem: AndroidProjectSystem): Boolean {
        return projectSystem is BlueprintProjectSystem;
    }
}

class BlueprintBuildSystemFilePreviewService : BuildSystemFilePreviewServices<BlueprintProjectSystem, BlueprintBuildTargetReference>, BlueprintToken {
    override fun isApplicable(buildTargetReference: BuildTargetReference): Boolean {
        return buildTargetReference is BlueprintBuildTargetReference
    }

    override fun subscribeBuildListener(project: Project, parentDisposable: Disposable, listener: BuildSystemFilePreviewServices.BuildListener) {
        throw UnsupportedOperationException()
    }

    override val buildTargets: BuildTargets = object : BuildTargets {
        override fun from(module: Module, targetFile: VirtualFile): BuildTargetReference {
            return BlueprintBuildTargetReference(module)
        }
    }

    override val buildServices: BuildSystemFilePreviewServices.BuildServices<BlueprintBuildTargetReference>
        get() = object : BuildSystemFilePreviewServices.BuildServices<BlueprintBuildTargetReference> {
            override fun getLastCompileStatus(buildTarget: BlueprintBuildTargetReference): ProjectSystemBuildManager.BuildStatus {
                return ProjectSystemBuildManager.BuildStatus.UNKNOWN
            }

            override fun buildArtifacts(buildTargets: Collection<BlueprintBuildTargetReference>) {
                throw UnsupportedOperationException()
            }
        }

    override fun getApplicationLiveEditServices(buildTargetReference: BlueprintBuildTargetReference): ApplicationLiveEditServices {
        throw UnsupportedOperationException()
    }

    override fun getRenderingServices(buildTargetReference: BlueprintBuildTargetReference): BuildSystemFilePreviewServices.RenderingServices {
        return BlueprintPreviewRenderingServices(buildTargetReference)
    }
}

class BlueprintBuildTargetReference(override val module: Module) : BuildTargetReference {
    override val moduleIfNotDisposed: Module? = if (module.isDisposed) null else module
}

class BlueprintPreviewRenderingServices(
    private val buildTarget: BlueprintBuildTargetReference,
) : BuildSystemFilePreviewServices.RenderingServices {
    override val classFileFinder: ClassFileFinder? = buildTarget.moduleIfNotDisposed?.let {
        BlueprintModuleClassFinder(it)
    }
}
