/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.StudioIcons
import javax.swing.Icon

class BlueprintFileType : LanguageFileType(BlueprintLanguage.INSTANCE) {
    override fun getName(): String = "Blueprint"

    override fun getDescription(): String = "Blueprint source file"

    override fun getDefaultExtension(): String = "bp"

    override fun getIcon(): Icon = StudioIcons.Common.ANDROID_HEAD

    companion object {
        @Suppress("unused") // Used in plugin.xml
        @JvmStatic
        val INSTANCE = BlueprintFileType()
    }
}