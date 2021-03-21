/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

private enum class BlueprintTypeGroup(val priority: Int) {
    Android(1000),
    Java(900),
    AIDL(800),
    Prebuilt(700),
    SysProp(600),
    AndroidTest(500),
    JavaHost(400),
    JavaTest(300),
    Other(1)
}

private data class BlueprintTypeCompletion(val name: String, val group: BlueprintTypeGroup, val weight: Double = 0.0, val desc: String = "")

private val BLUEPRINT_TYPES = arrayOf(
    BlueprintTypeCompletion("java_defaults", BlueprintTypeGroup.Java),
    BlueprintTypeCompletion("filegroup", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("java_library", BlueprintTypeGroup.Java, 1.0),
    BlueprintTypeCompletion("java_library_host", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("java_binary", BlueprintTypeGroup.Java),
    BlueprintTypeCompletion("java_binary_host", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("java_import_host", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("java_test", BlueprintTypeGroup.JavaTest),
    BlueprintTypeCompletion("java_test_host", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("java_test_helper_library", BlueprintTypeGroup.JavaTest),
    BlueprintTypeCompletion("java_genrule", BlueprintTypeGroup.Java),
    BlueprintTypeCompletion("java_genrule_host", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("java_sdk_library", BlueprintTypeGroup.Java, 0.8, "java_sdk_library is a special Java library that provides optional platform APIs to apps. In practice, it can be viewed as a combination of several modules: 1) stubs library that clients are linked against to, 2) droiddoc module that internally generates API stubs source files, 3) the real runtime shared library that implements the APIs, and 4) XML file for adding the runtime lib to the classpath at runtime if requested via <uses-library>."),
    BlueprintTypeCompletion("java_plugin", BlueprintTypeGroup.Java),
    BlueprintTypeCompletion("java_system_modules", BlueprintTypeGroup.Java),
    BlueprintTypeCompletion("java_library_static", BlueprintTypeGroup.Java, 1.0),
    BlueprintTypeCompletion("java_import", BlueprintTypeGroup.Java, 0.5, "imports one or more `.jar` files into the build graph as if they were built by a java_library module. By default, a java_import has a single variant that expects a `.jar` file containing `.class` files that were compiled against an Android classpath. Specifying `host_supported: true` will produce two variants, one for use as a dependency of device modules and one for host modules."),
    BlueprintTypeCompletion("javadoc", BlueprintTypeGroup.JavaHost),
    BlueprintTypeCompletion("genrule", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("genrule_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("gensrcs", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_binary_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_library_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("python_test_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("stubs_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("android_app", BlueprintTypeGroup.Android, 1.0, "Compiles sources and Android resources into an Android application package `.apk` file."),
    BlueprintTypeCompletion("android_library", BlueprintTypeGroup.Android, 1.0, "Builds and links sources into a `.jar` file for the device along with Android resources. An android_library has a single variant that produces a `.jar` file containing `.class` files that were compiled against the device bootclasspath, along with a `package-res.apk` file containing Android resources compiled with aapt2. This module is not suitable for installing on a device, but can be used as a `static_libs` dependency of an android_app module."),
    BlueprintTypeCompletion("android_test", BlueprintTypeGroup.AndroidTest, 0.8),
    BlueprintTypeCompletion("android_test_helper_app", BlueprintTypeGroup.AndroidTest, 0.5),
    BlueprintTypeCompletion("android_library_import", BlueprintTypeGroup.Android, 0.7, "android_library_import imports an `.aar` file into the build graph as if it was built with android_library. This module is not suitable for installing on a device, but can be used as a `static_libs` dependency of an android_app module."),
    BlueprintTypeCompletion("android_app_certificate", BlueprintTypeGroup.Android, 0.2, "android_app_certificate modules can be referenced by the certificates property of android_app modules to select the signing key."),
    BlueprintTypeCompletion("prebuilt_etc", BlueprintTypeGroup.Prebuilt, desc = "prebuilt_etc is for a prebuilt artifact that is installed in <partition>/etc/<sub_dir> directory."),
    BlueprintTypeCompletion("prebuilt_etc_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("doc_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("droidstubs", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("droiddoc", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("aidl_interface", BlueprintTypeGroup.AIDL),
    BlueprintTypeCompletion("aidl_mapping ", BlueprintTypeGroup.AIDL),
    BlueprintTypeCompletion("cc_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_binary_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_test_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_benchmark", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_benchmark_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library_host_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library_host_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_library_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_prebuilt_library_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_prebuilt_library_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_object", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_genrule", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_test_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("cc_prebuilt_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_library_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_cc_test_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_global_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_debug_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("libart_cc_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("sysprop_library", BlueprintTypeGroup.SysProp),
    BlueprintTypeCompletion("tradefed_binary_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("versioned_ndk_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("ndk_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("ndk_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("vndk_prebuilt_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llndk_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llvm_prebuilt_library_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llndk_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llvm_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llvm_tblgen", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llvm_host_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("llvm_host_prebuilt_library_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("libclang_rt_llndk_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("libclang_rt_prebuilt_library_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("libclang_rt_prebuilt_library_static", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("clang_builtin_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("clang_tblgen", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("force_build_llvm_components_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("phony", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("vts_config", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("hidl_interface", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("hidl_package_root", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("bootstrap_go_package", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("blueprint_go_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("bootstrap_go_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("fluoride_defaults_qti", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("perfHaldefaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("apex", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("apex_key", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("prebuilt_apex", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("apex_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("art_apex_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("apex_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("xsd_config", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("droiddoc_exported_dir", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("generate_mojom_pickles", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("generate_mojom_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("generate_mojom_srcs", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("generate_mojom_srcjar", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("sh_test", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("sh_binary", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("sh_binary_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("wayland_protocol_codegen", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("bpf", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("ca_certificates", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("ca_certificates_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("se_filegroup", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("se_cil_compat_map", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("target_fs_config_gen_filegroup", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("kernel_config", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("kernel_headers", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("prebuilt_apis", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("droidstubs_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("droiddoc_host", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("vintf_compatibility_matrix", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("soong_namespace", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("sanitizer_status_library_shared", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("fluoride_defaults", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("toolchain_library", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("ndk_prebuilt_object", BlueprintTypeGroup.Other),
    BlueprintTypeCompletion("libart_static_cc_default", BlueprintTypeGroup.Other),
)

class BlueprintTypeCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val mInsertHandler = BlueprintInsertHandler()

    private val mElements = BLUEPRINT_TYPES.map { blueprint ->
        LookupElementBuilder.create(blueprint.name).let {
            it.withInsertHandler(mInsertHandler)
        }.let {
            PrioritizedLookupElement.withPriority(it, blueprint.weight)
        }.let {
            PrioritizedLookupElement.withGrouping(it, blueprint.group.priority)
        }
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        result.addAllElements(mElements)
    }
}

private class BlueprintInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document

        document.insertString(context.tailOffset, " {\n    name: \"\",\n}")
        editor.caretModel.moveToOffset(context.tailOffset - 4)
    }
}
