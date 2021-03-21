/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This is a generated file. Not intended for manual editing.
package com.github.pvoid.androidbp.blueprint.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.github.pvoid.androidbp.blueprint.psi.*;
import com.github.pvoid.androidbp.blueprint.BlueprintPsiUtilsMirror;

public class BlueprintStringExprImpl extends ASTWrapperPsiElement implements BlueprintStringExpr {

  public BlueprintStringExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BlueprintVisitor visitor) {
    visitor.visitStringExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BlueprintVisitor) accept((BlueprintVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public String getValue() {
    return BlueprintPsiUtilsMirror.getValue(this);
  }

}
