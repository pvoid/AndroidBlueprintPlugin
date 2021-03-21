/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.android

import com.android.tools.idea.model.ClassJarProvider
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import java.io.File

class AospClassJarProvider : ClassJarProvider {
    override fun getModuleExternalLibraries(module: Module): List<File> {
        val libraries = ReadAction.compute<Array<Library>, Throwable> {
            LibraryTablesRegistrar.getInstance().getLibraryTable(module.project).libraries
        }

        return libraries.flatMap { library ->
            library.getFiles(OrderRootType.CLASSES).asSequence()
        }.map {
            File(it.path)
        }
    }
}