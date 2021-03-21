/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This is a generated file. Not intended for manual editing.
package com.github.pvoid.androidbp.blueprint.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class BlueprintVisitor extends PsiElementVisitor {

  public void visitArray(@NotNull BlueprintArray o) {
    visitPsiElement(o);
  }

  public void visitArrayElement(@NotNull BlueprintArrayElement o) {
    visitPsiElement(o);
  }

  public void visitArrayExpr(@NotNull BlueprintArrayExpr o) {
    visitPsiElement(o);
  }

  public void visitBlueprint(@NotNull BlueprintBlueprint o) {
    visitPsiElement(o);
  }

  public void visitBlueprintType(@NotNull BlueprintBlueprintType o) {
    visitPsiElement(o);
  }

  public void visitElements(@NotNull BlueprintElements o) {
    visitPsiElement(o);
  }

  public void visitFieldName(@NotNull BlueprintFieldName o) {
    visitPsiElement(o);
  }

  public void visitMembers(@NotNull BlueprintMembers o) {
    visitPsiElement(o);
  }

  public void visitObject(@NotNull BlueprintObject o) {
    visitPsiElement(o);
  }

  public void visitPair(@NotNull BlueprintPair o) {
    visitPsiElement(o);
  }

  public void visitStringExpr(@NotNull BlueprintStringExpr o) {
    visitPsiElement(o);
  }

  public void visitValue(@NotNull BlueprintValue o) {
    visitPsiElement(o);
  }

  public void visitVariable(@NotNull BlueprintVariable o) {
    visitPsiElement(o);
  }

  public void visitVariableRefExpr(@NotNull BlueprintVariableRefExpr o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
