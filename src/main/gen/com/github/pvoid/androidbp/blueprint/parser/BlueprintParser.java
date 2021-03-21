/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This is a generated file. Not intended for manual editing.
package com.github.pvoid.androidbp.blueprint.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.github.pvoid.androidbp.blueprint.psi.BlueprintTypes.*;
import static com.github.pvoid.androidbp.blueprint.parser.BlueprintParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class BlueprintParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return blueprintFile(b, l + 1);
  }

  /* ********************************************************** */
  // ARRAY_START elements? ARRAY_END
  public static boolean array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array")) return false;
    if (!nextTokenIs(b, ARRAY_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ARRAY_START);
    r = r && array_1(b, l + 1);
    r = r && consumeToken(b, ARRAY_END);
    exit_section_(b, m, ARRAY, r);
    return r;
  }

  // elements?
  private static boolean array_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_1")) return false;
    elements(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // STRING|BOOL|NUMBER|VARIABLE_NAME|LINK
  public static boolean array_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_element")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARRAY_ELEMENT, "<array element>");
    r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, BOOL);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, VARIABLE_NAME);
    if (!r) r = consumeToken(b, LINK);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // array (PLUS array)*
  public static boolean array_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_expr")) return false;
    if (!nextTokenIs(b, ARRAY_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = array(b, l + 1);
    r = r && array_expr_1(b, l + 1);
    exit_section_(b, m, ARRAY_EXPR, r);
    return r;
  }

  // (PLUS array)*
  private static boolean array_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!array_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "array_expr_1", c)) break;
    }
    return true;
  }

  // PLUS array
  private static boolean array_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLUS);
    r = r && array(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // blueprint_type OBJECT_START members? OBJECT_END
  public static boolean blueprint(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blueprint")) return false;
    if (!nextTokenIs(b, BLUEPRINT_TYPE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLUEPRINT, null);
    r = blueprint_type(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, OBJECT_START));
    r = p && report_error_(b, blueprint_2(b, l + 1)) && r;
    r = p && consumeToken(b, OBJECT_END) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // members?
  private static boolean blueprint_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blueprint_2")) return false;
    members(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // item_*
  static boolean blueprintFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blueprintFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "blueprintFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // BLUEPRINT_TYPE
  public static boolean blueprint_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blueprint_type")) return false;
    if (!nextTokenIs(b, BLUEPRINT_TYPE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, BLUEPRINT_TYPE);
    exit_section_(b, m, BLUEPRINT_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // array_element (ELEMENT_SEPARATOR array_element)* ELEMENT_SEPARATOR?
  public static boolean elements(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elements")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ELEMENTS, "<elements>");
    r = array_element(b, l + 1);
    r = r && elements_1(b, l + 1);
    r = r && elements_2(b, l + 1);
    exit_section_(b, l, m, r, false, BlueprintParser::elements_recover);
    return r;
  }

  // (ELEMENT_SEPARATOR array_element)*
  private static boolean elements_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elements_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!elements_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "elements_1", c)) break;
    }
    return true;
  }

  // ELEMENT_SEPARATOR array_element
  private static boolean elements_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elements_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELEMENT_SEPARATOR);
    r = r && array_element(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ELEMENT_SEPARATOR?
  private static boolean elements_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elements_2")) return false;
    consumeToken(b, ELEMENT_SEPARATOR);
    return true;
  }

  /* ********************************************************** */
  // !(ARRAY_END)
  static boolean elements_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elements_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, ARRAY_END);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // FIELD_NAME
  public static boolean field_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name")) return false;
    if (!nextTokenIs(b, FIELD_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FIELD_NAME);
    exit_section_(b, m, FIELD_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // blueprint|variable|COMMENT|WHITE_SPACE
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    r = blueprint(b, l + 1);
    if (!r) r = variable(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    if (!r) r = consumeToken(b, WHITE_SPACE);
    return r;
  }

  /* ********************************************************** */
  // pair (ELEMENT_SEPARATOR pair)* ELEMENT_SEPARATOR?
  public static boolean members(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "members")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MEMBERS, "<members>");
    r = pair(b, l + 1);
    r = r && members_1(b, l + 1);
    r = r && members_2(b, l + 1);
    exit_section_(b, l, m, r, false, BlueprintParser::members_recover);
    return r;
  }

  // (ELEMENT_SEPARATOR pair)*
  private static boolean members_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "members_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!members_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "members_1", c)) break;
    }
    return true;
  }

  // ELEMENT_SEPARATOR pair
  private static boolean members_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "members_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ELEMENT_SEPARATOR);
    r = r && pair(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ELEMENT_SEPARATOR?
  private static boolean members_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "members_2")) return false;
    consumeToken(b, ELEMENT_SEPARATOR);
    return true;
  }

  /* ********************************************************** */
  // !(OBJECT_END)
  static boolean members_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "members_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, OBJECT_END);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // OBJECT_START members? OBJECT_END
  public static boolean object(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object")) return false;
    if (!nextTokenIs(b, OBJECT_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OBJECT_START);
    r = r && object_1(b, l + 1);
    r = r && consumeToken(b, OBJECT_END);
    exit_section_(b, m, OBJECT, r);
    return r;
  }

  // members?
  private static boolean object_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_1")) return false;
    members(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // field_name EQUALS value
  public static boolean pair(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pair")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PAIR, "<pair>");
    r = field_name(b, l + 1);
    r = r && consumeToken(b, EQUALS);
    r = r && value(b, l + 1);
    exit_section_(b, l, m, r, false, BlueprintParser::pair_statement_recover);
    return r;
  }

  /* ********************************************************** */
  // !(WHITE_SPACE | ELEMENT_SEPARATOR | OBJECT_END)
  static boolean pair_statement_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pair_statement_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !pair_statement_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // WHITE_SPACE | ELEMENT_SEPARATOR | OBJECT_END
  private static boolean pair_statement_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pair_statement_recover_0")) return false;
    boolean r;
    r = consumeToken(b, WHITE_SPACE);
    if (!r) r = consumeToken(b, ELEMENT_SEPARATOR);
    if (!r) r = consumeToken(b, OBJECT_END);
    return r;
  }

  /* ********************************************************** */
  // STRING (PLUS STRING)*
  public static boolean string_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_expr")) return false;
    if (!nextTokenIs(b, STRING)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STRING);
    r = r && string_expr_1(b, l + 1);
    exit_section_(b, m, STRING_EXPR, r);
    return r;
  }

  // (PLUS STRING)*
  private static boolean string_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!string_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "string_expr_1", c)) break;
    }
    return true;
  }

  // PLUS STRING
  private static boolean string_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, PLUS, STRING);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // string_expr|array_expr|object|BOOL|NUMBER|variable_ref_expr
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, VALUE, "<value>");
    r = string_expr(b, l + 1);
    if (!r) r = array_expr(b, l + 1);
    if (!r) r = object(b, l + 1);
    if (!r) r = consumeToken(b, BOOL);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = variable_ref_expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // VARIABLE_NAME (PLUS_EQUALS|EQUALS) value
  public static boolean variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable")) return false;
    if (!nextTokenIs(b, VARIABLE_NAME)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, VARIABLE, null);
    r = consumeToken(b, VARIABLE_NAME);
    p = r; // pin = 1
    r = r && report_error_(b, variable_1(b, l + 1));
    r = p && value(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // PLUS_EQUALS|EQUALS
  private static boolean variable_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_1")) return false;
    boolean r;
    r = consumeToken(b, PLUS_EQUALS);
    if (!r) r = consumeToken(b, EQUALS);
    return r;
  }

  /* ********************************************************** */
  // VARIABLE_VALUE (PLUS value)?
  public static boolean variable_ref_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_ref_expr")) return false;
    if (!nextTokenIs(b, VARIABLE_VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VARIABLE_VALUE);
    r = r && variable_ref_expr_1(b, l + 1);
    exit_section_(b, m, VARIABLE_REF_EXPR, r);
    return r;
  }

  // (PLUS value)?
  private static boolean variable_ref_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_ref_expr_1")) return false;
    variable_ref_expr_1_0(b, l + 1);
    return true;
  }

  // PLUS value
  private static boolean variable_ref_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable_ref_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLUS);
    r = r && value(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

}
