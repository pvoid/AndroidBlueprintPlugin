/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

val JAVA_SDK_LIBRARY_FIELDS = listOf(
    BlueprintStringField("name", "The name of the module. Must be unique across all modules."),
    BlueprintStringListField("srcs", "list of source files used to compile the Java module. May be .java, .kt, .logtags, .proto, or .aidl files."),
    BlueprintStringListField("exclude_srcs", "list of source files that should not be used to build the Java module. This is most useful in the arch/multilib variants to remove non-common files"),
    BlueprintStringListField("defaults",""),
    BlueprintBooleanField("host_supported", "If set to true, build a variant of the module for the host. Defaults to false."),
    BlueprintBooleanField("device_supported", "If set to true, build a variant of the module for the device. Defaults to true."),
    BlueprintObjectField("aidl", "", listOf(
        BlueprintStringListField("include_dirs", "Top level directories to pass to aidl tool"),
        BlueprintStringListField("local_include_dirs", "Directories rooted at the Android.bp file to pass to aidl tool"),
        BlueprintStringListField("export_include_dirs", "directories that should be added as include directories for any aidl sources of modules that depend on this module, as well as to aidl for this module."),
        BlueprintBooleanField("generate_traces", "whether to generate traces (for systrace) for this interface"),
        BlueprintBooleanField("generate_get_transaction_name", "whether to generate Binder#GetTransaction name method."),
        BlueprintStringListField("flags", "list of flags that will be passed to the AIDL compiler"),
    )),
    BlueprintBooleanField("annotations_enabled", "is set to true, Metalava will allow framework SDK to contain annotations."),
    BlueprintStringListField("apex_available", "Availability of this module in APEXes. Only the listed APEXes can contain this module. If the module has stubs then other APEXes and the platform may access it through them (subject to visibility). \"//apex_available:anyapex\" is a pseudo APEX name that matches to any APEX. \"//apex_available:platform\" refers to non-APEX partitions like \"system.img\". \"com.android.gki.*\" matches any APEX module name with the prefix \"com.android.gki.\". Default is [\"//apex_available:platform\"]."),
    BlueprintStringListField("api_dir", "the relative path to the directory containing the api specification files. Defaults to \"api\"."),
    BlueprintObjectField("api_lint", "Properties related to api linting.", listOf(
        BlueprintBooleanField("enabled", "Enable api linting."),
    )),
    BlueprintBooleanField("api_only", "Determines whether a runtime implementation library is built; defaults to false. If true then it also prevents the module from being used as a shared module, i.e. it is as is shared_library: false, was set."),
    BlueprintStringListField("api_packages", "list of package names that will be documented and publicized as API. This allows the API to be restricted to a subset of the source files provided. If this is unspecified then all the source files will be treated as being part of the API."),
    BlueprintInterfaceField("arch", ""),
    BlueprintStringListField("common_srcs", "list Kotlin of source files containing Kotlin code that should be treated as common code in a codebase that supports Kotlin multiplatform. See https://kotlinlang.org/docs/reference/multiplatform.html. May be only be .kt files."),
    BlueprintBooleanField("compile_dex", "If set to true, compile dex regardless of installable. Defaults to false."),
    BlueprintStringField("compile_multilib", "control whether this module compiles for 32-bit, 64-bit, or both. Possible values are \"32\" (compile for 32-bit only), \"64\" (compile for 64-bit only), \"both\" (compile for both architectures), or \"first\" (compile for 64-bit on a 64-bit platform, and 32-bit on a 32-bit platform)."),
    BlueprintBooleanField("core_lib", "If set to true, the path of dist files is apistubs/core. Defaults to false."),
    BlueprintBooleanField("debug_ramdisk", "Whether this module is installed to debug ramdisk"),
    BlueprintBooleanField("default_to_stubs", "Determines if the stubs are preferred over the implementation library for linking, even when the client doesn't specify sdk_version. When this is set to true, such clients are provided with the widest API surface that this lib provides. Note however that this option doesn't affect the clients that are in the same APEX as this library. In that case, the clients are always linked with the implementation library. Default is false."),
    BlueprintBooleanField("device_specific", "whether this module is specific to a device, not only for SoC, but also for off-chip peripherals. When set to true, it is installed into /odm (or /vendor/odm if odm partition does not exist, or /system/vendor/odm if both odm and vendor partitions do not exist). This implies `soc_specific:true`."),
    BlueprintObjectField("dex_preopt", "", listOf(
        BlueprintBooleanField("enabled", "If false, prevent dexpreopting. Defaults to true."),
        BlueprintBooleanField("app_image", "If true, generate an app image (.art file) for this module."),
        BlueprintBooleanField("profile_guided", "If true, use a checked-in profile to guide optimization. Defaults to false unless a matching profile is set or a profile is found in PRODUCT_DEX_PREOPT_PROFILE_DIR that matches the name of this module, in which case it is defaulted to true."),
        BlueprintStringField("profile", "If set, provides the path to profile relative to the Android.bp file. If not set, defaults to searching for a file that matches the name of this module in the default profile location set by PRODUCT_DEX_PREOPT_PROFILE_DIR, or empty if not found."),
    )),
    BlueprintObjectField("dist", "configuration to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)", listOf(
        BlueprintStringListField("targets", "Copy the output of this module to the \$DIST_DIR when `dist` is specified on the command line and any of these targets are also on the command line, or otherwise built"),
        BlueprintStringField("dest", "The name of the output artifact. This defaults to the basename of the output of the module."),
        BlueprintStringListField("dir", "The directory within the dist directory to store the artifact. Defaults to the top level directory (\"\")."),
        BlueprintStringField("suffix", "A suffix to add to the artifact file name (before any extension)."),
        BlueprintStringField("tag", "A string tag to select the OutputFiles associated with the tag. If no tag is specified then it will select the default dist paths provided by the module type. If a tag of \"\" is specified then it will return the default output files provided by the modules, i.e. the result of calling OutputFiles(\"\")."),
    )),
    BlueprintStringField("dist_stem", "The stem for the artifacts that are copied to the dist, if not specified then defaults to the base module name. For each scope the following artifacts are copied to the apistubs/<scope> directory in the dist. * stubs impl jar -> <dist-stem>.jar * API specification file -> api/<dist-stem>.txt * Removed API specification file -> api/<dist-stem>-removed.txt Also used to construct the name of the filegroup (created by prebuilt_apis) that references the latest released API and remove API specification files. * API specification filegroup -> <dist-stem>.api.<scope>.latest * Removed API specification filegroup -> <dist-stem>-removed.api.<scope>.latest * API incompatibilities baseline filegroup -> <dist-stem>-incompatibilities.api.<scope>.latest"),
    BlueprintStringListField("dists", "a list of configurations to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)"),
    BlueprintStringListField("doctag_files", "Files containing information about supported java doc tags."),
    BlueprintStringListField("droiddoc_option_files", "local files that are used within user customized droiddoc options."),
    BlueprintStringListField("droiddoc_options", """additional droiddoc options Available variables for substitution:

$(location <label>): the path to the droiddoc_option_files with name <label>
"""),
    BlueprintStringListField("dxflags", "list of module-specific flags that will be used for dex compiles"),
    BlueprintBooleanField("enabled", "emit build rules for this module Disabling a module should only be done for those modules that cannot be built in the current environment. Modules that can build in the current environment but are not usually required (e.g. superceded by a prebuilt) should not be disabled as that will prevent them from being built by the checkbuild target and so prevent early detection of changes that have broken those modules."),
    BlueprintBooleanField("enforce_uses_libs", "If true, the list of uses_libs and optional_uses_libs modules must match the AndroidManifest.xml file. Defaults to true if either uses_libs or optional_uses_libs is set. Will unconditionally default to true in the future."),
    BlueprintObjectField("errorprone", "", listOf(
        BlueprintStringListField("javacflags", "List of javac flags that should only be used when running errorprone."),
        BlueprintStringListField("extra_check_modules", "List of java_plugin modules that provide extra errorprone checks."),
    )),
    BlueprintStringListField("exclude_java_resource_dirs", "list of directories that should be excluded from java_resource_dirs"),
    BlueprintStringListField("exclude_java_resources", "list of files that should be excluded from java_resources and java_resource_dirs"),
    BlueprintStringListField("exported_plugins", "List of modules to export to libraries that directly depend on this library as annotation processors. Note that if the plugins set generates_api: true this will disable the turbine optimization on modules that depend on this module, which will reduce parallelism and cause more recompilation."),
    BlueprintStringListField("hidden_api_packages", "list of package names that must be hidden from the API"),
    BlueprintStringListField("hiddenapi_additional_annotations", "A list of java_library instances that provide additional hiddenapi annotations for the library."),
    BlueprintLibrariesField("host_required", "names of other modules to install on host if this module is installed"),
    BlueprintBooleanField("hostdex", "If true, export a copy of the module as a -hostdex module for host testing."),
    BlueprintStringListField("impl_library_visibility", "Visibility for impl library module. If not specified then defaults to the visibility property."),
    BlueprintLibrariesField("impl_only_libs", "List of Java libraries that will be in the classpath when building the implementation lib"),
    BlueprintBooleanField("include_srcs", "If set to true, include sources used to compile the module in to the final jar"),
    BlueprintStringListField("init_rc", "init.rc files to be installed if this module is installed"),
    BlueprintBooleanField("installable", "If set to true, allow this module to be dexed and installed on devices. Has no effect on host modules, which are always considered installable. Default: true"),
    BlueprintObjectField("jacoco", "", listOf(
        BlueprintStringListField("include_filter", "List of classes to include for instrumentation with jacoco to collect coverage information at runtime when building with coverage enabled. If unset defaults to all classes. Supports '*' as the last character of an entry in the list as a wildcard match. If preceded by '.' it matches all classes in the package and subpackages, otherwise it matches classes in the package that have the class name as a prefix."),
        BlueprintStringListField("exclude_filter", "List of classes to exclude from instrumentation with jacoco to collect coverage information at runtime when building with coverage enabled. Overrides classes selected by the include_filter property. Supports '*' as the last character of an entry in the list as a wildcard match. If preceded by '.' it matches all classes in the package and subpackages, otherwise it matches classes in the package that have the class name as a prefix."),
    )),
    BlueprintStringField("jarjar_rules", "if not blank, run jarjar using the specified rules file"),
    BlueprintStringListField("java_resource_dirs", "list of directories containing Java resources"),
    BlueprintStringListField("java_resources", "list of files to use as Java resources"),
    BlueprintStringField("java_version", "If not blank, set the java version passed to javac as -source and -target"),
    BlueprintNumberField("javac_shard_size", "The number of Java source entries each Javac instance can process"),
    BlueprintStringListField("javacflags", "list of module-specific flags that will be used for javac compiles"),
    BlueprintStringListField("kotlincflags", "list of module-specific flags that will be used for kotlinc compiles"),
    BlueprintLibrariesField("libs", "list of java libraries that will be in the classpath"),
    BlueprintStringListField("licenses", "Describes the licenses applicable to this module. Must reference license modules."),
    BlueprintObjectField("lint", "Controls for running Android Lint on the module.", listOf(
        BlueprintBooleanField("enabled",  "If true, run Android Lint on the module. Defaults to true."),
        BlueprintStringListField("flags",  "Flags to pass to the Android Lint tool."),
        BlueprintStringListField("fatal_checks",  "Checks that should be treated as fatal."),
        BlueprintStringListField("error_checks",  "Checks that should be treated as errors."),
        BlueprintStringListField("warning_checks",  "Checks that should be treated as warnings."),
        BlueprintStringListField("disabled_checks",  "Checks that should be skipped."),
        BlueprintStringListField("extra_check_modules",  "Modules that provide extra lint checks"),
        BlueprintStringListField("baseline_filename",  "Name of the file that lint uses as the baseline. Defaults to \"lint-baseline.xml\"."),
    )),
    BlueprintStringField("manifest", "manifest file to be included in resulting jar"),
    BlueprintStringListField("merge_annotations_dirs", "a list of top-level directories containing files to merge qualifier annotations (i.e. those intended to be included in the stubs written) from."),
    BlueprintStringListField("merge_inclusion_annotations_dirs", "a list of top-level directories containing Java stub files to merge show/hide annotations from."),
    BlueprintStringField("min_sdk_version", "if not blank, set the minimum version of the sdk that the compiled artifacts will run against. Defaults to sdk_version if not set."),
    BlueprintStringField("multilib", ""),
    BlueprintStringField("naming_scheme", "The naming scheme to use for the components that this module creates. If not specified then it defaults to \"default\". This is a temporary mechanism to simplify conversion from separate modules for each component that follow a different naming pattern to the default one. TODO(b/155480189) - Remove once naming inconsistencies have been resolved."),
    BlueprintBooleanField("native_bridge_supported", "Whether this module is built for non-native architectures (also known as native bridge binary)"),
    BlueprintBooleanField("no_dist", "If set to true then don't create dist rules."),
    BlueprintStringField("notice", "relative path to a file to include in the list of notices for the device"),
    BlueprintObjectField("openjdk9", "", listOf(
        BlueprintStringListField("srcs", "List of source files that should only be used when passing -source 1.9 or higher"),
        BlueprintStringListField("javacflags", "List of javac flags that should only be used when passing -source 1.9 or higher"),
    )),
    BlueprintObjectField("optimize", "", listOf(
        BlueprintBooleanField("enabled", "If false, disable all optimization. Defaults to true for android_app and android_test modules, false for java_library and java_test modules."),
        BlueprintBooleanField("proguard_compatibility", "If true, runs R8 in Proguard compatibility mode (default). Otherwise, runs R8 in full mode."),
        BlueprintBooleanField("shrink", "If true, optimize for size by removing unused code. Defaults to true for apps, false for libraries and tests."),
        BlueprintBooleanField("optimize", "If true, optimize bytecode. Defaults to false."),
        BlueprintBooleanField("obfuscate", "If true, obfuscate bytecode. Defaults to false."),
        BlueprintBooleanField("no_aapt_flags", "If true, do not use the flag files generated by aapt that automatically keep classes referenced by the app manifest. Defaults to false."),
        BlueprintStringListField("proguard_flags", "Flags to pass to proguard."),
        BlueprintStringListField("proguard_flags_files", "Specifies the locations of files containing proguard flags."),
    )),
    BlueprintLibrariesField("optional_uses_libs", "A list of shared library modules that will be listed in uses-library tags in the AndroidManifest.xml file with required=false."),
    BlueprintStringField("owner", "vendor who owns this module"),
    BlueprintStringField("patch_module", "When compiling language level 9+ .java code in packages that are part of a system module, patch_module names the module that your sources and dependencies should be patched into. The Android runtime currently doesn't implement the JEP 261 module system so this option is only supported at compile time. It should only be needed to compile tests in packages that exist in libcore and which are inconvenient to move elsewhere." ),
    BlueprintStringListField("permitted_packages", "If not empty, classes are restricted to the specified packages and their sub-packages. This restriction is checked after applying jarjar rules and including static libs."),
    BlueprintBooleanField("platform_apis", "Whether to compile against the platform APIs instead of an SDK. If true, then sdk_version must be empty. The value of this field is ignored when module's type isn't android_app."),
    BlueprintStringListField("plugins", "List of modules to use as annotation processors"),
    BlueprintBooleanField("product_specific", "whether this module is specific to a software configuration of a product (e.g. country, network operator, etc). When set to true, it is installed into /product (or /system/product if product partition does not exist)."),
    BlueprintBooleanField("proprietary", "whether this is a proprietary vendor module, and should be installed into /vendor"),
    BlueprintObjectField("proto", "", listOf(
        BlueprintStringListField("output_params", "List of extra options that will be passed to the proto generator."),
        BlueprintStringField("type", "Proto generator type. C++: full or lite. Java: micro, nano, stream, or lite."),
        BlueprintStringField("plugin", "Proto plugin to use as the generator. Must be a cc_binary_host module."),
        BlueprintStringListField("include_dirs", "list of directories that will be added to the protoc include paths."),
        BlueprintStringListField("local_include_dirs", "list of directories relative to the bp file that will be added to the protoc include paths."),
        BlueprintBooleanField("canonical_path_from_root", "whether to identify the proto files from the root of the source tree (the original method in Android, useful for android-specific protos), or relative from where they were specified (useful for external/third party protos). This defaults to true today, but is expected to default to false in the future."),
    )),
    BlueprintStringField("provides_uses_lib", "Optional name of the <uses-library> provided by this module. This is needed for non-SDK libraries, because SDK ones are automatically picked up by Soong. The <uses-library> name normally is the same as the module name, but there are exceptions."),
    BlueprintObjectField("public", "The properties specific to the public api scope Unless explicitly specified by using public.enabled the public api scope is enabled by default in both legacy and non-legacy mode. The properties specific to the system api scope In legacy mode the system api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the system api scope is disabled by default. The properties specific to the test api scope In legacy mode the test api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the test api scope is disabled by default. The properties specific to the module-lib api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default. The properties specific to the system-server api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default.", listOf(
        BlueprintBooleanField("enabled", "Indicates whether the api surface is generated. If this is set for any scope then all scopes must explicitly specify if they are enabled. This is to prevent new usages from depending on legacy behavior. Otherwise, if this is not set for any scope then the default behavior is scope specific so please refer to the scope specific property documentation."),
        BlueprintStringField("sdk_version", """The sdk_version to use for building the stubs. If not specified then it will use an sdk_version determined as follows: 1) If the sdk_version specified on the java_sdk_library is none then this

will be none. This is used for java_sdk_library instances that are used
to create stubs that contribute to the core_current sdk version.
2) Otherwise, it is assumed that this library extends but does not contribute

directly to a specific sdk_version and so this uses the sdk_version appropriate
for the api scope. e.g. public will use sdk_version: current, system will use
sdk_version: system_current, etc.

This does not affect the sdk_version used for either generating the stubs source or the API file. They both have to use the same sdk_version as is used for compiling the implementation library.""")
    )),
    BlueprintObjectField("system", "The properties specific to the public api scope Unless explicitly specified by using public.enabled the public api scope is enabled by default in both legacy and non-legacy mode. The properties specific to the system api scope In legacy mode the system api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the system api scope is disabled by default. The properties specific to the test api scope In legacy mode the test api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the test api scope is disabled by default. The properties specific to the module-lib api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default. The properties specific to the system-server api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default.", listOf(
        BlueprintBooleanField("enabled", "Indicates whether the api surface is generated. If this is set for any scope then all scopes must explicitly specify if they are enabled. This is to prevent new usages from depending on legacy behavior. Otherwise, if this is not set for any scope then the default behavior is scope specific so please refer to the scope specific property documentation."),
        BlueprintStringField("sdk_version", """The sdk_version to use for building the stubs. If not specified then it will use an sdk_version determined as follows: 1) If the sdk_version specified on the java_sdk_library is none then this

will be none. This is used for java_sdk_library instances that are used
to create stubs that contribute to the core_current sdk version.
2) Otherwise, it is assumed that this library extends but does not contribute

directly to a specific sdk_version and so this uses the sdk_version appropriate
for the api scope. e.g. public will use sdk_version: current, system will use
sdk_version: system_current, etc.

This does not affect the sdk_version used for either generating the stubs source or the API file. They both have to use the same sdk_version as is used for compiling the implementation library.""")
    )),
    BlueprintObjectField("test", "The properties specific to the public api scope Unless explicitly specified by using public.enabled the public api scope is enabled by default in both legacy and non-legacy mode. The properties specific to the system api scope In legacy mode the system api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the system api scope is disabled by default. The properties specific to the test api scope In legacy mode the test api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the test api scope is disabled by default. The properties specific to the module-lib api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default. The properties specific to the system-server api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default.", listOf(
        BlueprintBooleanField("enabled", "Indicates whether the api surface is generated. If this is set for any scope then all scopes must explicitly specify if they are enabled. This is to prevent new usages from depending on legacy behavior. Otherwise, if this is not set for any scope then the default behavior is scope specific so please refer to the scope specific property documentation."),
        BlueprintStringField("sdk_version", """The sdk_version to use for building the stubs. If not specified then it will use an sdk_version determined as follows: 1) If the sdk_version specified on the java_sdk_library is none then this

will be none. This is used for java_sdk_library instances that are used
to create stubs that contribute to the core_current sdk version.
2) Otherwise, it is assumed that this library extends but does not contribute

directly to a specific sdk_version and so this uses the sdk_version appropriate
for the api scope. e.g. public will use sdk_version: current, system will use
sdk_version: system_current, etc.

This does not affect the sdk_version used for either generating the stubs source or the API file. They both have to use the same sdk_version as is used for compiling the implementation library.""")
    )),
    BlueprintObjectField("module_lib", "The properties specific to the public api scope Unless explicitly specified by using public.enabled the public api scope is enabled by default in both legacy and non-legacy mode. The properties specific to the system api scope In legacy mode the system api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the system api scope is disabled by default. The properties specific to the test api scope In legacy mode the test api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the test api scope is disabled by default. The properties specific to the module-lib api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default. The properties specific to the system-server api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default.", listOf(
        BlueprintBooleanField("enabled", "Indicates whether the api surface is generated. If this is set for any scope then all scopes must explicitly specify if they are enabled. This is to prevent new usages from depending on legacy behavior. Otherwise, if this is not set for any scope then the default behavior is scope specific so please refer to the scope specific property documentation."),
        BlueprintStringField("sdk_version", """The sdk_version to use for building the stubs. If not specified then it will use an sdk_version determined as follows: 1) If the sdk_version specified on the java_sdk_library is none then this

will be none. This is used for java_sdk_library instances that are used
to create stubs that contribute to the core_current sdk version.
2) Otherwise, it is assumed that this library extends but does not contribute

directly to a specific sdk_version and so this uses the sdk_version appropriate
for the api scope. e.g. public will use sdk_version: current, system will use
sdk_version: system_current, etc.

This does not affect the sdk_version used for either generating the stubs source or the API file. They both have to use the same sdk_version as is used for compiling the implementation library.""")
    )),
    BlueprintObjectField("system_server", "The properties specific to the public api scope Unless explicitly specified by using public.enabled the public api scope is enabled by default in both legacy and non-legacy mode. The properties specific to the system api scope In legacy mode the system api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the system api scope is disabled by default. The properties specific to the test api scope In legacy mode the test api scope is enabled by default when sdk_version is set to something other than \"none\". In non-legacy mode the test api scope is disabled by default. The properties specific to the module-lib api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default. The properties specific to the system-server api scope Unless explicitly specified by using test.enabled the module-lib api scope is disabled by default.", listOf(
        BlueprintBooleanField("enabled", "Indicates whether the api surface is generated. If this is set for any scope then all scopes must explicitly specify if they are enabled. This is to prevent new usages from depending on legacy behavior. Otherwise, if this is not set for any scope then the default behavior is scope specific so please refer to the scope specific property documentation."),
        BlueprintStringField("sdk_version", """The sdk_version to use for building the stubs. If not specified then it will use an sdk_version determined as follows: 1) If the sdk_version specified on the java_sdk_library is none then this

will be none. This is used for java_sdk_library instances that are used
to create stubs that contribute to the core_current sdk version.
2) Otherwise, it is assumed that this library extends but does not contribute

directly to a specific sdk_version and so this uses the sdk_version appropriate
for the api scope. e.g. public will use sdk_version: current, system will use
sdk_version: system_current, etc.

This does not affect the sdk_version used for either generating the stubs source or the API file. They both have to use the same sdk_version as is used for compiling the implementation library.""")
    )),
    BlueprintBooleanField("ramdisk", "Whether this module is installed to ramdisk"),
    BlueprintBooleanField("recovery", "Whether this module is installed to recovery partition"),
    BlueprintLibrariesField("required", "names of other modules to install if this module is installed"),
    BlueprintStringField("sdk_version", "if not blank, set to the version of the sdk to compile against. Defaults to compiling against the current platform."),
    BlueprintStringListField("services", "List of files to include in the META-INF/services folder of the resulting jar."),
    BlueprintBooleanField("shared_library", "Specifies whether this module can be used as an Android shared library; defaults to true. An Android shared library is one that can be referenced in a <uses-library> element in an AndroidManifest.xml."),
    BlueprintBooleanField("soc_specific", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist)."),
    BlueprintBooleanField("static_kotlin_stdlib", "If true, package the kotlin stdlib into the jar. Defaults to true."),
    BlueprintLibrariesField("static_libs", "list of java libraries that will be compiled into the resulting jar"),
    BlueprintStringField("stem", "set the name of the output"),
    BlueprintLibrariesField("stub_only_libs", "List of Java libraries that will be in the classpath when building stubs"),
    BlueprintStringListField("stubs_library_visibility", "Visibility for stubs library modules. If not specified then defaults to the visibility property."),
    BlueprintStringListField("stubs_source_visibility", "Visibility for stubs source modules. If not specified then defaults to the visibility property."),
    BlueprintBooleanField("system_ext_specific", "whether this module extends system. When set to true, it is installed into /system_ext (or /system/system_ext if system_ext partition does not exist)."),
    BlueprintStringField("system_modules", "When targeting 1.9 and above, override the modules to use with --system, otherwise provides defaults libraries to add to the bootclasspath."),
    BlueprintObjectField("target", "", listOf(
        BlueprintObjectField("host","", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("android", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("hostdex", "", listOf(
            BlueprintLibrariesField("required", "Additional required dependencies to add to -hostdex modules.")
        )),
    )),
    BlueprintLibrariesField("target_required", "names of other modules to install on target if this module is installed"),
    BlueprintStringField("target_sdk_version", "if not blank, set the targetSdkVersion in the AndroidManifest.xml. Defaults to sdk_version if not set."),
    BlueprintBooleanField("uncompress_dex", "Keep the data uncompressed. We always need uncompressed dex for execution, so this might actually save space by avoiding storing the same data twice. This defaults to reasonable value based on module and should not be set. It exists only to support ART tests."),
    BlueprintBooleanField("unsafe_ignore_missing_latest_api", "A compatibility mode that allows historical API-tracking files to not exist. Do not use."),
    BlueprintBooleanField("use_tools_jar", "Add host jdk tools.jar to bootclasspath"),
    BlueprintStringListField("uses_libs", "A list of shared library modules that will be listed in uses-library tags in the AndroidManifest.xml file."),
    BlueprintBooleanField("v4_signature", "If true, generate the signature file of APK Signing Scheme V4, along side the signed APK file. Defaults to false."),
    BlueprintBooleanField("vendor", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist). Use `soc_specific` instead for better meaning."),
    BlueprintBooleanField("vendor_ramdisk", "Whether this module is installed to vendor ramdisk"),
    BlueprintStringListField("vintf_fragments", "VINTF manifest fragments to be installed if this module is installed"),
    BlueprintStringListField("visibility", "Controls the visibility of this module to other modules."),
)