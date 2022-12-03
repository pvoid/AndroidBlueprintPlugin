/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.apk.analyzer.AaptInvoker
import com.android.tools.idea.log.LogWrapper
import com.android.tools.idea.model.ClassJarProvider
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
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.util.messages.MessageBus
import org.jetbrains.android.dom.manifest.getPrimaryManifestXml
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.AndroidRootUtil
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.io.File
import java.nio.file.Path

const val AOSP_PROJECT_SYSTEM_ID = "com.github.pvoid.androidbp.AospAndroidProjectSystem"

class AospProjectSystemProvider(val project: Project) : AndroidProjectSystemProvider {
    override val id: String = AOSP_PROJECT_SYSTEM_ID

    override val projectSystem: AndroidProjectSystem by lazy {
        AospAndroidProjectSystem(project)
    }

    override fun isApplicable(): Boolean {
        return AospProjectHelper.blueprintFileForProject(project) != null
    }
}

private class AospAndroidProjectSystem(
    private val mProject: Project
) : AndroidProjectSystem, BlueprintsProvider {

    private val mSyncManager = AospProjectSystemSyncManager(mProject)

    private val moduleSystems = mutableMapOf<Module, AndroidModuleSystem>()

    private val sourceProvidersFactory = object : SourceProvidersFactory {

        private val providers = mutableMapOf<AndroidFacet, SourceProviders>()

        override fun createSourceProvidersFor(facet: AndroidFacet): SourceProviders {
            return providers.getOrPut(facet) {
                if (mSyncManager.isSyncNeeded()) {
                    mProject.getSyncManager().syncProject(ProjectSystemSyncManager.SyncReason.PROJECT_MODIFIED)
                }

                val sdk =
                    ProjectRootManager.getInstance(mProject).projectSdk?.takeIf { it.sdkType.name == AOSP_SDK_TYPE_NAME }
                BlueprintSourceProviders(mProject.basePath, sdk, blueprints)
            }
        }
    }

    @Suppress("unused")
    private val mListener = BlueprintChangeListener(mProject.messageBus, mSyncManager::isFileWatched)

    override fun getDefaultApkFile(): VirtualFile? = null // TODO: Link to a file in output?

    override fun allowsFileCreation(): Boolean = false

    override fun getPathToAapt(): Path {
        return AaptInvoker.getPathToAapt(
            AndroidSdks.getInstance().tryToChooseSdkHandler(),
            LogWrapper(AospAndroidProjectSystem::class.java)
        )
    }

    override fun getSyncManager(): ProjectSystemSyncManager = mSyncManager

    override fun getAndroidFacetsWithPackageName(
        project: Project,
        packageName: String
    ): Collection<AndroidFacet> {
        return ProjectFacetManager.getInstance(project)
            .getFacets(AndroidFacet.ID)
            .asSequence()
            .filter { it.getPrimaryManifestXml()?.packageName == packageName }
            .toList()
    }

    override fun getClassJarProvider(): ClassJarProvider {
        return object: ClassJarProvider {
            override fun getModuleExternalLibraries(module: Module): List<File> {
                return AndroidRootUtil.getExternalLibraries(module).map { file: VirtualFile? -> VfsUtilCore.virtualToIoFile(file!!) }
            }

            override fun isClassFileOutOfDate(module: Module, fqcn: String, classFile: VirtualFile): Boolean {
                return false
            }
        }
    }

    override fun getModuleSystem(module: Module): AndroidModuleSystem = moduleSystems.getOrPut(module) {
        AospAndroidModuleSystem(mProject, module, mSyncManager)
    }

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

    override fun getSourceProvidersFactory(): SourceProvidersFactory = sourceProvidersFactory

    override fun getApkProvider(runConfiguration: RunConfiguration): ApkProvider? {
        return null
    }

    override val blueprints: List<Blueprint>
        get() = AospProjectHelper.blueprintFileForProject(mProject)?.let {
            BlueprintsTable.get(it)
        }?.filter(AospProjectHelper::shouldHaveFacet) ?: emptyList()

    override fun getApplicationIdProvider(runConfiguration: RunConfiguration): ApplicationIdProvider {
        return mProject.allModules().first {
            ModuleType.get(it).id == ModuleTypeId.JAVA_MODULE
        }.let {
            BlueprintIdProvider(it, this)
        }
    }

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