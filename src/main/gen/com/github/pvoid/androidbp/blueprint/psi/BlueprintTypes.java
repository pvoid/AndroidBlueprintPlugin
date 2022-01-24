/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This is a generated file. Not intended for manual editing.
package com.github.pvoid.androidbp.blueprint.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.github.pvoid.androidbp.blueprint.BlueprintElementType;
import com.github.pvoid.androidbp.blueprint.BlueprintTokenType;
import com.github.pvoid.androidbp.blueprint.impl.*;

public interface BlueprintTypes {

  IElementType DUMNMY = new BlueprintElementType("DUMMY");
  IElementType ARRAY = new BlueprintElementType("ARRAY");
  IElementType ARRAY_ELEMENT = new BlueprintElementType("ARRAY_ELEMENT");
  IElementType ARRAY_EXPR = new BlueprintElementType("ARRAY_EXPR");
  IElementType BLUEPRINT = new BlueprintElementType("BLUEPRINT");
  IElementType BLUEPRINT_TYPE = new BlueprintElementType("BLUEPRINT_TYPE");
  IElementType ELEMENTS = new BlueprintElementType("ELEMENTS");
  IElementType FIELD_NAME = new BlueprintElementType("FIELD_NAME");
  IElementType MEMBERS = new BlueprintElementType("MEMBERS");
  IElementType OBJECT = new BlueprintElementType("OBJECT");
  IElementType PAIR = new BlueprintElementType("PAIR");
  IElementType STRING_EXPR = new BlueprintElementType("STRING_EXPR");
  IElementType VALUE = new BlueprintElementType("VALUE");
  IElementType VARIABLE = new BlueprintElementType("VARIABLE");
  IElementType VARIABLE_REF_EXPR = new BlueprintElementType("VARIABLE_REF_EXPR");

  IElementType ARRAY_END = new BlueprintTokenType("]");
  IElementType ARRAY_START = new BlueprintTokenType("[");
  IElementType BOOL = new BlueprintTokenType("BOOL");
  IElementType COMMENT = new BlueprintTokenType("COMMENT");
  IElementType ELEMENT_SEPARATOR = new BlueprintTokenType(",");
  IElementType EQUALS = new BlueprintTokenType("EQUALS");
  IElementType LINK = new BlueprintTokenType("LINK");
  IElementType NUMBER = new BlueprintTokenType("NUMBER");
  IElementType OBJECT_END = new BlueprintTokenType("}");
  IElementType OBJECT_START = new BlueprintTokenType("{");
  IElementType PLUS = new BlueprintTokenType("+");
  IElementType PLUS_EQUALS = new BlueprintTokenType("+=");
  IElementType STRING = new BlueprintTokenType("STRING");
  IElementType VARIABLE_NAME = new BlueprintTokenType("VARIABLE_NAME");
  IElementType VARIABLE_VALUE = new BlueprintTokenType("VARIABLE_VALUE");
  IElementType WHITE_SPACE = new BlueprintTokenType("WHITE_SPACE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARRAY) {
        return new BlueprintArrayImpl(node);
      }
      else if (type == ARRAY_ELEMENT) {
        return new BlueprintArrayElementImpl(node);
      }
      else if (type == ARRAY_EXPR) {
        return new BlueprintArrayExprImpl(node);
      }
      else if (type == BLUEPRINT) {
        return new BlueprintBlueprintImpl(node);
      }
      else if (type == BLUEPRINT_TYPE) {
        return new BlueprintBlueprintTypeImpl(node);
      }
      else if (type == ELEMENTS) {
        return new BlueprintElementsImpl(node);
      }
      else if (type == FIELD_NAME) {
        return new BlueprintFieldNameImpl(node);
      }
      else if (type == MEMBERS) {
        return new BlueprintMembersImpl(node);
      }
      else if (type == OBJECT) {
        return new BlueprintObjectImpl(node);
      }
      else if (type == PAIR) {
        return new BlueprintPairImpl(node);
      }
      else if (type == STRING_EXPR) {
        return new BlueprintStringExprImpl(node);
      }
      else if (type == VALUE) {
        return new BlueprintValueImpl(node);
      }
      else if (type == VARIABLE) {
        return new BlueprintVariableImpl(node);
      }
      else if (type == VARIABLE_REF_EXPR) {
        return new BlueprintVariableRefExprImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
