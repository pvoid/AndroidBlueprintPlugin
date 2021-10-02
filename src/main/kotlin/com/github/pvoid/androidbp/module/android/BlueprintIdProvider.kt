/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.idea.run.ApplicationIdProvider
import com.github.pvoid.androidbp.blueprint.model.JavaSdkLibraryBlueprint
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.util.text.nullize
import org.jetbrains.android.dom.manifest.cachedValueFromPrimaryManifest
import org.jetbrains.android.facet.AndroidFacet

private val PACKAGE_NAME = Key.create<CachedValue<String?>>("merged.manifest.package.name")

class BlueprintIdProvider(
    private val mModule: Module,
    private val mBlueprintsProvider: BlueprintsProvider
) : ApplicationIdProvider {
    // TODO: Parse from blueprint
    override fun getPackageName(): String {
        return mBlueprintsProvider.blueprints.mapNotNull { (it as? JavaSdkLibraryBlueprint)?.apiPackages?.firstOrNull() }.firstOrNull() ?:
            getFromManifest()
    }

    override fun getTestPackageName(): String? = null

    private fun getFromManifest(): String {
        val facet = AndroidFacet.getInstance(mModule) ?: throw AssertionError()
        val cachedValue = facet.cachedValueFromPrimaryManifest {
            packageName.nullize(true)
        }
        return facet.putUserDataIfAbsent(PACKAGE_NAME, cachedValue).value ?: throw AssertionError()
    }
}