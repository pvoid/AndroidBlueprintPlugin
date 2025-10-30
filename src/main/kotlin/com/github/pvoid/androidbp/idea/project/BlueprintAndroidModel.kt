/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.lint.detector.api.Desugaring
import org.jetbrains.android.facet.AndroidFacet

class BlueprintAndroidModel(
    private val facet: AndroidFacet
) : AndroidModel {

    private val platformVersion: Int by lazy {
        facet.module.project.guessPlatformVersion() ?: 1
    }

    override val isDebuggable: Boolean = true

    override val allApplicationIds: Set<String>
        get() = setOf(applicationId)

    override val applicationId: String by lazy {
        val moduleSystem = facet.getModuleSystem() as? BlueprintModuleSystem
        moduleSystem?.getPackageName() ?: "com.example"
    }

    override val desugaring: Set<Desugaring> = Desugaring.Companion.NONE

    override val minSdkVersion: AndroidVersion
        get() = AndroidVersion(platformVersion, null)

    override val namespacing: Namespacing = Namespacing.DISABLED

    override val runtimeMinSdkVersion: AndroidVersion
        get() = AndroidVersion(platformVersion, null)

    override val targetSdkVersion: AndroidVersion?
        get() = AndroidVersion(platformVersion, null)

    override fun overridesManifestPackage(): Boolean = false

    companion object {
        fun register(facet: AndroidFacet) {
            AndroidModel.Companion.set(facet, BlueprintAndroidModel(facet))
        }
    }
}