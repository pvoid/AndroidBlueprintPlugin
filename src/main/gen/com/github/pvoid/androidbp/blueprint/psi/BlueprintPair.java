/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This is a generated file. Not intended for manual editing.
package com.github.pvoid.androidbp.blueprint.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.github.pvoid.androidbp.blueprint.completion.fields.BlueprintField;

public interface BlueprintPair extends PsiElement {

  @NotNull
  BlueprintFieldName getFieldName();

  @NotNull
  BlueprintValue getValue();

  BlueprintField getFieldDef();

}
