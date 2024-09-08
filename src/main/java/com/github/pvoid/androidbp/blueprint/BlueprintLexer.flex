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
BLUEPRIN_NAME=(java_defaults|
               java_aconfig_library|
               aconfig_declarations|
               filegroup|
               java_library|
               java_library_host|
               java_binary|
               java_binary_host|
               java_import_host|
               java_test|
               java_test_host|
               java_test_helper_library|
               java_genrule|
               java_genrule_host|
               java_sdk_library|
               java_plugin|
               java_system_modules|
               javadoc|
               genrule|
               genrule_defaults|
               gensrcs|
               python_defaults|
               python_binary_host|
               python_library_host|
               python_library|
               python_test|
               python_test_host|
               stubs_defaults|
               java_import|
               android_app|
               android_app_certificate|
               android_library|
               android_library_import|
               android_test_helper_app|
               android_test|
               aidl_interface|
               prebuilt_etc|
               prebuilt_etc_host|
               doc_defaults|
               droidstubs|
               droiddoc|
               java_library_static|
               aidl_mapping |
               cc_defaults|
               cc_binary |
               cc_binary_host|
               cc_test |
               cc_test_library |
               cc_library|
               cc_benchmark|
               cc_benchmark_host|
               cc_library_static |
               cc_library_host_static|
               cc_library_host_shared|
               cc_library_shared|
               cc_library_headers |
               cc_prebuilt_library_shared |
               cc_prebuilt_library_static |
               cc_object |
               cc_genrule |
               cc_test_host|
               cc_prebuilt_binary|
               art_cc_defaults|
               art_cc_binary|
               art_cc_library|
               art_cc_library_static|
               art_cc_test|
               art_cc_test_library|
               art_global_defaults|
               art_debug_defaults|
               libart_cc_defaults|
               sysprop_library|
               tradefed_binary_host|
               versioned_ndk_headers |
               ndk_headers |
               ndk_library |
               vndk_prebuilt_shared|
               llndk_library |
               llvm_prebuilt_library_static|
               llndk_headers|
               llvm_defaults|
               llvm_tblgen|
               llvm_host_defaults|
               llvm_host_prebuilt_library_shared|
               libclang_rt_llndk_library|
               libclang_rt_prebuilt_library_shared|
               libclang_rt_prebuilt_library_static|
               clang_builtin_headers|
               clang_tblgen|
               force_build_llvm_components_defaults|
               phony|
               vts_config |
               hidl_interface |
               hidl_package_root |
               bootstrap_go_package|
               blueprint_go_binary|
               bootstrap_go_binary|
               fluoride_defaults_qti|
               perfHaldefaults|
               apex|
               apex_key|
               prebuilt_apex|
               apex_defaults|
               art_apex_test|
               apex_test|
               xsd_config|
               droiddoc_exported_dir|
               generate_mojom_pickles|
               generate_mojom_headers|
               generate_mojom_srcs|
               generate_mojom_srcjar|
               sh_test|
               sh_binary|
               sh_binary_host|
               wayland_protocol_codegen|
               bpf|
               ca_certificates|
               ca_certificates_host|
               se_filegroup|
               se_cil_compat_map|
               target_fs_config_gen_filegroup|
               kernel_config|
               kernel_headers|
               prebuilt_apis|
               droidstubs_host|
               droiddoc_host|
               vintf_compatibility_matrix|
               soong_namespace|
               sanitizer_status_library_shared|
               fluoride_defaults|
               toolchain_library|
               ndk_prebuilt_object|
               libart_static_cc_defaults|
               package|
               license|
               license_kind|
               csuite_test|
               csuite_config|
               makefile_goal|
               prebuilt_build_tool|
               soong_config_bool_variable|
               soong_config_module_type|
               soong_config_module_type_import|
               soong_config_string_variable|
               soong_namespace
               )
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
