/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

val ANDROID_LIBRARY_FIELDS = listOf(
    BlueprintStringField("name", "The name of the module. Must be unique across all modules."),
    BlueprintStringListField("srcs", "list of source files used to compile the Java module. May be .java, .logtags, .proto, or .aidl files."),
    BlueprintBooleanField("aapt_include_all_resources", "include all resource configurations, not just the product-configured ones."),
    BlueprintStringListField("aaptflags", "flags passed to aapt when creating the apk"),
    BlueprintObjectField("aidl", "", listOf(
        BlueprintStringListField("include_dirs", "Top level directories to pass to aidl tool"),
        BlueprintStringListField("local_include_dirs", "Directories rooted at the Android.bp file to pass to aidl tool"),
        BlueprintStringListField("export_include_dirs", "directories that should be added as include directories for any aidl sources of modules that depend on this module, as well as to aidl for this module."),
        BlueprintBooleanField("generate_traces", "whether to generate traces (for systrace) for this interface"),
        BlueprintBooleanField("generate_get_transaction_name", "whether to generate Binder#GetTransaction name method."),
    )),
    BlueprintStringListField("asset_dirs", "list of directories relative to the Blueprints file containing assets. Defaults to [\"assets\"] if a directory called assets exists. Set to [] to disable the default."),
    BlueprintBooleanField("compile_dex", "If set to true, compile dex regardless of installable. Defaults to false."),
    BlueprintStringField("compile_multilib", "control whether this module compiles for 32-bit, 64-bit, or both. Possible values are \"32\" (compile for 32-bit only), \"64\" (compile for 64-bit only), \"both\" (compile for both architectures), or \"first\" (compile for 64-bit on a 64-bit platform, and 32-bit on a 32-bit platform"),
    BlueprintStringListField("defaults", ""),
    BlueprintBooleanField("device_specific", "whether this module is specific to a device, not only for SoC, but also for off-chip peripherals. When set to true, it is installed into /odm (or /vendor/odm if odm partition does not exist, or /system/vendor/odm if both odm and vendor partitions do not exist). This implies `soc_specific:true`."),
    BlueprintObjectField("dex_preopt", "", listOf(
        BlueprintBooleanField("enabled", "If false, prevent dexpreopting and stripping the dex file from the final jar. Defaults to true."),
        BlueprintBooleanField("no_stripping", "If true, never strip the dex files from the final jar when dexpreopting. Defaults to false."),
        BlueprintBooleanField("app_image", "If true, generate an app image (.art file) for this module."),
        BlueprintBooleanField("profile_guided", "If true, use a checked-in profile to guide optimization. Defaults to false unless a matching profile is set or a profile is found in PRODUCT_DEX_PREOPT_PROFILE_DIR that matches the name of this module, in which case it is defaulted to true."),
        BlueprintStringField("profile", "If set, provides the path to profile relative to the Android.bp file. If not set, defaults to searching for a file that matches the name of this module in the default profile location set by PRODUCT_DEX_PREOPT_PROFILE_DIR, or empty if not found."),
    )),
    BlueprintObjectField("dist", "", listOf(
        BlueprintStringListField("targets", "copy the output of this module to the \$DIST_DIR when `dist` is specified on the command line and any of these targets are also on the command line, or otherwise built"),
        BlueprintStringField("dest", "The name of the output artifact. This defaults to the basename of the output of the module."),
        BlueprintStringField("dir", "The directory within the dist directory to store the artifact. Defaults to the top level directory (\"\")."),
        BlueprintStringField("suffix", "A suffix to add to the artifact file name (before any extension)."),
    )),
    BlueprintStringListField("dxflags", "list of module-specific flags that will be used for dex compiles"),
    BlueprintBooleanField("enabled", "emit build rules for this module"),
    BlueprintObjectField("errorprone", "", listOf(
        BlueprintStringListField("javacflags", "List of javac flags that should only be used when running errorprone."),
    )),
    BlueprintStringListField("exclude_java_resource_dirs", "list of directories that should be excluded from java_resource_dirs"),
    BlueprintStringListField("exclude_java_resources", "list of files that should be excluded from java_resources and java_resource_dirs"),
    BlueprintStringListField("exclude_srcs", "list of source files that should not be used to build the Java module. This is most useful in the arch/multilib variants to remove non-common files"),
    BlueprintBooleanField("hostdex", "If true, export a copy of the module as a -hostdex module for host testing."),
    BlueprintBooleanField("include_srcs", "If set to true, include sources used to compile the module in to the final jar"),
    BlueprintStringListField("init_rc", "init.rc files to be installed if this module is installed"),
    BlueprintBooleanField("installable", "If set to true, allow this module to be dexed and installed on devices. Has no effect on host modules, which are always considered installable."),
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
    BlueprintLibrariesListField("libs", "list of of java libraries that will be in the classpath"),
    BlueprintStringField("manifest", "path to AndroidManifest.xml. If unset, defaults to \"AndroidManifest.xml\"."),
    BlueprintStringField("min_sdk_version", "if not blank, set the minimum version of the sdk that the compiled artifacts will run against. Defaults to sdk_version if not set."),
    BlueprintBooleanField("no_framework_libs", "don't build against the framework libraries (ext, and framework for device targets)"),
    BlueprintBooleanField("no_standard_libs", "don't build against the default libraries (bootclasspath, ext, and framework for device targets)"),
    BlueprintStringField("notice", "relative path to a file to include in the list of notices for the device"),
    BlueprintObjectField("openjdk9", "", listOf(
        BlueprintStringListField("srcs", "List of source files that should only be used when passing -source 1.9"),
        BlueprintStringListField("javacflags", "List of javac flags that should only be used when passing -source 1.9"),
    )),
    BlueprintObjectField("optimize", "", listOf(
        BlueprintBooleanField("enabled", "If false, disable all optimization. Defaults to true for android_app and android_test modules, false for java_library and java_test modules."),
        BlueprintBooleanField("enabledByDefault", "True if the module containing this has it set by default."),
        BlueprintBooleanField("shrink", "If true, optimize for size by removing unused code. Defaults to true for apps, false for libraries and tests."),
        BlueprintBooleanField("optimize", "If true, optimize bytecode. Defaults to false."),
        BlueprintBooleanField("obfuscate", "If true, obfuscate bytecode. Defaults to false."),
        BlueprintBooleanField("no_aapt_flags", "If true, do not use the flag files generated by aapt that automatically keep classes referenced by the app manifest. Defaults to false."),
        BlueprintStringListField("proguard_flags", "Flags to pass to proguard."),
        BlueprintStringListField("proguard_flags_files", "Specifies the locations of files containing proguard flags."),
    )),
    BlueprintStringField("owner", "vendor who owns this module"),
    BlueprintStringField("patch_module", "When compiling language level 9+ .java code in packages that are part of a system module, patch_module names the module that your sources and dependencies should be patched into. The Android runtime currently doesn't implement the JEP 261 module system so this option is only supported at compile time. It should only be needed to compile tests in packages that exist in libcore and which are inconvenient to move elsewhere."),
    BlueprintStringListField("permitted_packages", "If not empty, classes are restricted to the specified packages and their sub-packages. This restriction is checked after applying jarjar rules and including static libs."),
    BlueprintBooleanField("platform_apis", "if true, compile against the platform APIs instead of an SDK."),
    BlueprintStringListField("plugins", "List of modules to use as annotation processors"),
    BlueprintBooleanField("product_services_specific", "whether this module provides services owned by the OS provider to the core platform. When set to true, it is installed into /product_services (or /system/product_services if product_services partition does not exist)."),
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
    BlueprintBooleanField("recovery", "Whether this module is installed to recovery partition"),
    BlueprintStringListField("required", "names of other modules to install if this module is installed"),
    BlueprintStringListField("resource_dirs", "list of directories relative to the Blueprints file containing Android resources. Defaults to [\"res\"] if a directory called res exists. Set to [] to disable the default."),
    BlueprintStringListField("resource_zips", "list of zip files containing Android resources."),
    BlueprintStringField("sdk_version", "if not blank, set to the version of the sdk to compile against. Defaults to compiling against the current sdk if platform_apis is not set."),
    BlueprintStringListField("services", "List of files to include in the META-INF/services folder of the resulting jar."),
    BlueprintBooleanField("soc_specific", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist)."),
    BlueprintLibrariesListField("static_libs", "list of java libraries that will be compiled into the resulting jar"),
    BlueprintStringField("system_modules", "When targeting 1.9, override the modules to use with --system"),
    BlueprintObjectField("target", "", listOf(
        BlueprintObjectField("host", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("android", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
    )),
    BlueprintStringField("target_sdk_version", "if not blank, set the targetSdkVersion in the AndroidManifest.xml. Defaults to sdk_version if not set."),
    BlueprintBooleanField("use_tools_jar", "Add host jdk tools.jar to bootclasspath"),
    BlueprintBooleanField("vendor", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist). Use `soc_specific` instead for better meaning."),
    BlueprintStringListField("vintf_fragments", "VINTF manifest fragments to be installed if this module is installed"),
)