/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.ui;

import com.intellij.openapi.projectRoots.Sdk;

import javax.swing.*;

public class AospSdkPanel {
    private JPanel rootPanel;
    private AospSdkCombobox aospSdkCombobox;

    public JComponent getRoot() {
        return rootPanel;
    }

    private void createUIComponents() {
        aospSdkCombobox = new AospSdkCombobox();
    }

    public String getSdkName() {
        final Sdk selectedSdk = aospSdkCombobox.getSelectedSdk();
        return selectedSdk == null ? null : selectedSdk.getName();
    }

    public Sdk getSdk() {
        return aospSdkCombobox.getSelectedSdk();
    }

    public void setSdk(Sdk sdk) {
        aospSdkCombobox.getComboBox().setSelectedItem(sdk);
    }
}
