/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module

import com.github.pvoid.androidbp.ui.AndroidBpWizardStep
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilderListener
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VirtualFile

class AndroidBpModuleBuilder : ModuleBuilder(), ModuleBuilderListener {

    var sdk: Sdk? = null

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
    }

    override fun getModuleType(): ModuleType<*> = ModuleTypeManager.getInstance().findByID(MODULE_TYPE_ID)

    override fun moduleCreated(module: Module) {
    }

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        return arrayOf(AndroidBpWizardStep(this))
    }
}