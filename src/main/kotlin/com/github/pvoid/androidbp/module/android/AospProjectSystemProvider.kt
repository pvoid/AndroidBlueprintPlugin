/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.apk.analyzer.AaptInvoker
import com.android.tools.idea.log.LogWrapper
import com.android.tools.idea.navigator.getSubmodules
import com.android.tools.idea.project.DefaultBuildManager
import com.android.tools.idea.projectsystem.*
import com.android.tools.idea.res.AndroidInnerClassFinder
import com.android.tools.idea.res.AndroidManifestClassPsiElementFinder
import com.android.tools.idea.res.AndroidResourceClassPsiElementFinder
import com.android.tools.idea.res.ProjectLightResourceClassService
import com.android.tools.idea.run.ApkProvider
import com.android.tools.idea.run.ApplicationIdProvider
import com.android.tools.idea.sdk.AndroidSdks
import com.github.pvoid.androidbp.blueprint.model.*
import com.github.pvoid.androidbp.module.AospProjectHelper
import com.github.pvoid.androidbp.module.sdk.AOSP_SDK_TYPE_NAME
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.facet.ProjectFacetManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.AppUIUtil
import com.intellij.util.messages.MessageBus
import org.jetbrains.android.dom.manifest.getPackageName
import org.jetbrains.android.facet.AndroidFacet
import java.nio.file.Path

val AOSP_PROJECT_SYSTEM_ID = "com.github.pvoid.androidbp.AospAndroidProjectSystem"

class AospProjectSystemProvider(val project: Project) : AndroidProjectSystemProvider {
    override val id: String = AOSP_PROJECT_SYSTEM_ID

    override val projectSystem: AndroidProjectSystem by lazy {
        AospAndroidProjectSystem(project, null)
    }

    override fun isApplicable(): Boolean {
        return AospProjectHelper.blueprintFileForProject(project) != null
    }
}

private class AospAndroidProjectSystem(
    private val mProject: Project,
    private val mIdProvider: ApplicationIdProvider?
) : AndroidProjectSystem {

    private val mSyncManager = AospProjectSystemSyncManager(mProject)

    private val mListener = BlueprintChangeListener(mProject.messageBus, mSyncManager::isFileWatched)

    override fun getDefaultApkFile(): VirtualFile? = null // TODO: Link to a file in output?

    override fun allowsFileCreation(): Boolean = false

    override fun getPathToAapt(): Path {
        return AaptInvoker.getPathToAapt(AndroidSdks.getInstance().tryToChooseSdkHandler(), LogWrapper(AospAndroidProjectSystem::class.java))
    }

    override fun getSyncManager(): ProjectSystemSyncManager = mSyncManager

    override fun getAndroidFacetsWithPackageName(project: Project, packageName: String, scope: GlobalSearchScope): Collection<AndroidFacet> {
        return ProjectFacetManager.getInstance(project)
            .getFacets(AndroidFacet.ID)
            .asSequence()
            .filter { getPackageName(it) == packageName }
            .filter { facet -> facet.sourceProviders.mainManifestFile?.let(scope::contains) == true }
            .toList()
    }

    override fun getModuleSystem(module: Module): AndroidModuleSystem = AospAndroidModuleSystem(mProject, module, mSyncManager)

    override fun getPsiElementFinders(): List<PsiElementFinder> {
        return listOf(
            AndroidInnerClassFinder.INSTANCE,
            AndroidManifestClassPsiElementFinder.getInstance(mProject),
            AndroidResourceClassPsiElementFinder(getLightResourceClassService())
        )
    }

    override fun getLightResourceClassService() = ProjectLightResourceClassService.getInstance(mProject)

    override val submodules: Collection<Module>
        get() = getSubmodules(mProject, null)

    override fun getSourceProvidersFactory(): SourceProvidersFactory = object : SourceProvidersFactory {
        override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
            if (mSyncManager.isSyncNeeded()) {
                mProject.getSyncManager().syncProject(ProjectSystemSyncManager.SyncReason.PROJECT_MODIFIED)
            }

            val sdk = ProjectRootManager.getInstance(mProject).projectSdk?.takeIf { it.sdkType.name == AOSP_SDK_TYPE_NAME }
            return BlueprintSourceProviders(mProject.basePath, sdk, mSyncManager.blueprints)
        }
    }

    override fun getApkProvider(runConfiguration: RunConfiguration): ApkProvider? {
        return null
    }

    override fun getApplicationIdProvider(runConfiguration: RunConfiguration): ApplicationIdProvider? = mIdProvider

    override fun getBuildManager(): ProjectSystemBuildManager = DefaultBuildManager

    inner class BlueprintChangeListener(
        bus: MessageBus,
        private val checker: (String) -> Boolean
    ) : BlueprintChangeNotifier, Disposable {

        private val mConnection = bus.connect(this)

        init {
            Disposer.register(mProject, this)
            mConnection.subscribe(BlueprintChangeNotifier.CHANGE_TOPIC, this)
        }

        override fun beforeAction(file: VirtualFile, blueprints: List<Blueprint>) {
        }

        override fun afterAction(file: VirtualFile, blueprints: List<Blueprint>) {
            if (!checker(file.url)) {
                return
            }

            mSyncManager.setSyncRequired()

            if (mSyncManager.isSyncNeeded()) {
                mProject.getSyncManager().syncProject(ProjectSystemSyncManager.SyncReason.PROJECT_MODIFIED)
            }
        }

        override fun dispose() {
            mConnection.dispose()
        }
    }
}