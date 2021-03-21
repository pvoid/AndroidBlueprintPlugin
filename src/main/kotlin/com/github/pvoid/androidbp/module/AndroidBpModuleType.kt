/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

const val MODULE_TYPE_ID = "ANDROID_BP_MODULE_TYPE"

val MODULE_TYPE_ICON by lazy {
    IconLoader.getIcon("/icons/module-type-icon.svg", AndroidBpModuleType::class.java)
}

class AndroidBpModuleType : ModuleType<AndroidBpModuleBuilder>(MODULE_TYPE_ID) {

    override fun createModuleBuilder(): AndroidBpModuleBuilder = AndroidBpModuleBuilder()

    override fun getName(): String = "Android Blue Print"

    override fun getDescription(): String = "AOSP blueprint based project"

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return MODULE_TYPE_ICON
    }
}