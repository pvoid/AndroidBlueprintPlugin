package com.github.pvoid.androidbp.blueprint;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;

import com.github.pvoid.androidbp.blueprint.parser.BlueprintSymbolFactory;
import static com.github.pvoid.androidbp.blueprint.parser.BlueprintSymbolFactory.Token.*;

%%

%{
  private final Stack<Integer> stack = new Stack<>();

  private BlueprintSymbolFactory factory;

  public BlueprintLexer() {
    this((java.io.Reader)null);
    factory = BlueprintSymbolFactory.create();
  }

  public BlueprintLexer(java.io.Reader reader, BlueprintSymbolFactory fcatory) {
    this(reader);
    this.factory = fcatory;
  }

  private void yypushstate(int state) {
    stack.push(yystate());
    yybegin(state);
  }

  private int yypopstate() {
    int state = stack.pop();
    yybegin(state);
    return state;
  }

  private int yytopstate() {
      return stack.peek();
  }

  private void yyclearstack() {
      stack.clear();
  }

  private boolean skipSpaces() {
      return factory.skipSpaces();
  }

  private <T extends IElementType> T create(BlueprintSymbolFactory.Token type) {
      return factory.<T>create(type, yytext());
  }

  private boolean mValueExpected = false;
%}

%public
%class BlueprintLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state YYCOMMENT

%state YYBLUEPRINT_START
%state YYBLUEPRINT
%state YYBLUEPRINT_VALUE_START
%state YYVALUE_START
%state YYVALUE
%state YYARRAY
%state YYOBJECT
%state YYOBJECT_VALUE_START
%state YYVARIABLE_START
%state PROPERTY_VALUE

%init{
yybegin(YYINITIAL);
%init}

EOL=\R
CRLF=\R
WHITE_SPACE=\s+
END_OF_LINE_COMMENT=(\/\/)[^\r\n]*
BLUEPRIN_NAME=[a-z_]+
VARIABLE_NAME=[a-zA-Z][a-zA-Z\-_0-9]*
DOUBLE_QUOTE_STRING=\"([^\"\n\f] | \\\")*\"
SINGLE_QUOTE_STRING=\'([^\'\n\f] | \\\')*\'
DOUBLE_QUOTE_LINK=\"\:([^\"\n\f] | \\\")*\"
SINGLE_QUOTE_LINK=\'\:([^\'\n\f] | \\\')*\'
NUMBER=[0-9]

%%
<YYINITIAL> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    {BLUEPRIN_NAME}            { yybegin(YYBLUEPRINT_START); return create(BLUEPRINT_TYPE); }
    {VARIABLE_NAME}            { yybegin(YYVARIABLE_START); return create(VARIABLE_NAME); }
}

<YYCOMMENT> {
    "*/"                       {
                                 yypopstate();
                                 if (!skipSpaces()) {
                                     return create(COMMENT);
                                 }
                               }
    [^*\n]+                    { if (!skipSpaces()) return create(COMMENT); }
    "*"                        { if (!skipSpaces()) return create(COMMENT); }
    {CRLF}                     { if (!skipSpaces()) return create(COMMENT); }
}

<YYBLUEPRINT_START> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    \{                         { yybegin(YYBLUEPRINT); return create(OBJECT_START); }
    [^]                        { return create(ERROR); }
}

<YYVARIABLE_START> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    \+\s*\=                    { yypushstate(YYVALUE); mValueExpected = true; return create(PLUS_EQUALS); }
    "="                        { yypushstate(YYVALUE); mValueExpected = true; return create(EQUALS); }
    [^]                        { return create(ERROR); }
}

<YYBLUEPRINT> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    {VARIABLE_NAME}+           { yypushstate(YYBLUEPRINT_VALUE_START); return create(FIELD_NAME); }
    \}                         { yybegin(YYINITIAL); return create(OBJECT_END); }
    [^]                        { return create(ERROR); }
}

<YYBLUEPRINT_VALUE_START> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    ":"                        { yybegin(YYVALUE); mValueExpected = true;  return create(EQUALS); }
    [^]                        { return create(ERROR); }
}

<YYVALUE> {
    "/*"                       { yypushstate(YYCOMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ","                        {
                                 if (yytopstate() == YYBLUEPRINT || yytopstate() == YYOBJECT) {
                                    yypopstate();
                                    return create(ELEMENT_SEPARATOR);
                                 } else {
                                    return create(ERROR);
                                 }
                               }
    "+"                        {
                                  if (mValueExpected) {
                                      return create(ERROR);
                                  }
                                  mValueExpected = true;
                                  return create(PLUS);
                               }
    \}                         {
                                 switch (yypopstate()) {
                                     case YYBLUEPRINT:
                                         yyclearstack();
                                         yybegin(YYINITIAL);
                                         return create(OBJECT_END);
                                     case YYOBJECT:
                                         yypopstate();
                                         return create(OBJECT_END);
                                 }
                                 return create(ERROR);
                               }
    \[                         { yypushstate(YYARRAY); mValueExpected = false; return create(ARRAY_START); }
    \{                         { yypushstate(YYOBJECT); mValueExpected = false; return create(OBJECT_START); }
    {NUMBER}+                  { mValueExpected = false; return create(NUMBER); }
    (true|false)               { mValueExpected = false; return create(BOOL); }
    {DOUBLE_QUOTE_STRING}      { mValueExpected = false; return create(STRING); }
    {SINGLE_QUOTE_STRING}      { mValueExpected = false; return create(STRING); }
    {BLUEPRIN_NAME}            {
                                 if (mValueExpected) {
                                    mValueExpected = false;
                                    return create(VARIABLE_VALUE);
                                 }

                                 if (yypopstate() == YYVARIABLE_START) {
                                    yyclearstack();
                                    yybegin(YYBLUEPRINT_START);
                                    return create(BLUEPRINT_TYPE);
                                 }

                                 return create(ERROR);
                               }
    {VARIABLE_NAME}            {
                                 if (mValueExpected) {
                                     mValueExpected = false;
                                     return create(VARIABLE_VALUE);
                                 }

                                 if (yypopstate() == YYVARIABLE_START) {
                                     yyclearstack();
                                     yybegin(YYVARIABLE_START);
                                     return create(VARIABLE_NAME);
                                 }

                                 return create(ERROR);
                               }
    [^]                        { return create(ERROR); }
}

<YYARRAY> {
    "/*"                       { yypushstate(YYCOMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    \]                         { yypopstate(); return create(ARRAY_END); }
    ,                          { return create(ELEMENT_SEPARATOR); }
    {NUMBER}+                  { return create(NUMBER); }
    (true|false)               { return create(BOOL); }
    {DOUBLE_QUOTE_LINK}        { return create(LINK); }
    {SINGLE_QUOTE_LINK}        { return create(LINK); }
    {DOUBLE_QUOTE_STRING}      { return create(STRING); }
    {SINGLE_QUOTE_STRING}      { return create(STRING); }
    {VARIABLE_NAME}            { return create(VARIABLE_VALUE); }
    [^]                        { return create(ERROR); }
}

<YYOBJECT> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    {VARIABLE_NAME}+           { yypushstate(YYOBJECT_VALUE_START); return create(FIELD_NAME); }
    \}                         { yypopstate(); return create(OBJECT_END); }
    ,                          { return create(ELEMENT_SEPARATOR); }
    [^]                        { return create(ERROR); }
}

<YYOBJECT_VALUE_START> {
    "/*"                       { yypushstate(YYCOMMENT); }
    {END_OF_LINE_COMMENT}      { if (!skipSpaces()) return create(COMMENT); }
    ({CRLF}|{WHITE_SPACE})     { if (!skipSpaces()) return create(WHITE_SPACE); }
    ":"                        { yybegin(YYVALUE); mValueExpected = true; return create(EQUALS); }
    [^]                        { return create(ERROR); }
}


[^] { return create(ERROR); }
