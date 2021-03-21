/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.ui;

import com.github.pvoid.androidbp.module.sdk.AospSdkType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.ui.ProjectJdksEditor;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class AospSdkCombobox extends ComboboxWithBrowseButton {
    public AospSdkCombobox() {
        getComboBox().setRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                if (value instanceof Sdk) {
                    append(((Sdk) value).getName());
                } else {
                    append("Select AOSP Source Code", SimpleTextAttributes.ERROR_ATTRIBUTES);
                }
            }
        });
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Sdk selectedSdk = getSelectedSdk();
                final Project project = ProjectManager.getInstance().getDefaultProject();
                ProjectJdksEditor editor = new ProjectJdksEditor(selectedSdk, project, AospSdkCombobox.this);
                editor.show();
                if (editor.isOK()) {
                    selectedSdk = editor.getSelectedJdk();
                    updateSdkList(selectedSdk, false);
                }
            }
        });
        updateSdkList(null, true);
    }

    public void updateSdkList(Sdk sdkToSelect, boolean selectAnySdk) {
        final List<Sdk> sdkList = ProjectJdkTable.getInstance().getSdksOfType(SdkType.findInstance(AospSdkType.class));
        if (selectAnySdk && sdkList.size() > 0) {
            sdkToSelect = sdkList.get(0);
        }
        sdkList.add(0, null);
        getComboBox().setModel(new DefaultComboBoxModel(sdkList.toArray(new Sdk[sdkList.size()])));
        getComboBox().setSelectedItem(sdkToSelect);
    }

    public Sdk getSelectedSdk() {
        return (Sdk) getComboBox().getSelectedItem();
    }
}
