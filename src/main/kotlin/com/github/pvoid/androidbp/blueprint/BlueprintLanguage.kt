/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.github.pvoid.androidbp.module.AndroidBpModuleType
import com.github.pvoid.androidbp.module.MODULE_TYPE_ICON
import com.intellij.lang.Language
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

val FILE_TYPE_ICON by lazy {
    IconLoader.getIcon("/icons/file-type-icon.svg", AndroidBpModuleType::class.java)
}

class BlueprintLanguage private constructor() : Language("Blueprint") {
    companion object {
        val INSTANCE = BlueprintLanguage()
    }
}

class BlueprintFileType : LanguageFileType(BlueprintLanguage.INSTANCE) {
    override fun getName(): String = "Blueprint"

    override fun getDescription(): String = "Blueprint source file"

    override fun getDefaultExtension(): String = "bp"

    override fun getIcon(): Icon = FILE_TYPE_ICON

    companion object {
        @JvmStatic
        val INSTANCE = BlueprintFileType()
    }
}

@Suppress("DEPRECATION")
class BlueprintFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(BlueprintFileType.INSTANCE)
    }
}

class BlueprintLexerAdapter : FlexAdapter(BlueprintLexer())