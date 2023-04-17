/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project.deprecated

import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.projectsystem.getModuleSystem
import com.android.tools.lint.detector.api.Desugaring
import com.github.pvoid.androidbp.idea.project.BlueprintModuleSystem
import com.github.pvoid.androidbp.idea.project.guessPlatformVersion
import org.jetbrains.android.facet.AndroidFacet

@Deprecated("Deprecated, but still used by some parts of android plugin")
class BlueprintAndroidModel(
    private val facet: AndroidFacet
) : AndroidModel {

    private val platformVersion: Int by lazy {
        facet.module.project.guessPlatformVersion() ?: 1
    }

    override fun getApplicationId(): String {
        val moduleSystem = facet.getModuleSystem() as? BlueprintModuleSystem ?: return "com.example"
        return moduleSystem.getPackageName() ?: "com.example"
    }

    override fun getAllApplicationIds(): Set<String> = setOf(applicationId)

    override fun overridesManifestPackage(): Boolean = false

    override fun isDebuggable(): Boolean = true

    override fun getMinSdkVersion(): AndroidVersion = AndroidVersion(platformVersion, null)

    override fun getRuntimeMinSdkVersion(): AndroidVersion = AndroidVersion(platformVersion, null)

    override fun getTargetSdkVersion(): AndroidVersion = AndroidVersion(platformVersion, null)

    override fun getNamespacing(): Namespacing = Namespacing.DISABLED

    override fun getDesugaring(): Set<Desugaring> = Desugaring.NONE

    companion object {
        fun register(facet: AndroidFacet) {
            AndroidModel.set(facet, BlueprintAndroidModel(facet))
        }
    }
}