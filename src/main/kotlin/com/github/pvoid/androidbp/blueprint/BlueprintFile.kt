/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class BlueprintFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BlueprintLanguage.INSTANCE) {

    override fun getFileType(): FileType = BlueprintFileType.INSTANCE

    override fun toString(): String = "Blueprint File"
}
