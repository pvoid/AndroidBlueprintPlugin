/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.ui

import com.github.pvoid.androidbp.module.AndroidBpModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import javax.swing.JComponent

class AndroidBpWizardStep(
    private val mModuleBuilder: AndroidBpModuleBuilder
) : ModuleWizardStep() {

    private val mSdkPanel = AospSdkPanel()

    override fun getComponent(): JComponent = mSdkPanel.root

    override fun updateDataModel() {
        mModuleBuilder.sdk = mSdkPanel.sdk
    }

    override fun validate(): Boolean {
        if (mSdkPanel.sdkName.isNullOrBlank()) {
            return false
        }

        return super.validate()
    }
}