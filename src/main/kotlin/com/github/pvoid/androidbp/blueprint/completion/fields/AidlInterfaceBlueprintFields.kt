/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

val AIDL_INTERFACE_FIELDS = listOf(
    BlueprintStringField("name", "The name of the module. Must be unique across all modules."),
    BlueprintStringListField("srcs", "List of .aidl files which compose this interface."),
    BlueprintBooleanField("host_supported", "Whether the library can be used on host"),
    BlueprintObjectField("backend", "", listOf(
        BlueprintObjectField("cpp","Backend of the compiler generating code for C++ clients using libbinder (unstable C++ interface) When enabled, this creates a target called \"<name>-cpp\".", listOf(
            BlueprintBooleanField("gen_log", "Whether to generate additional code for gathering information about the transactions. Default: false"),
            BlueprintBooleanField("enabled", "Whether to generate code in the corresponding backend. Default: true"),
            BlueprintBooleanField("apex_available", ""),
            BlueprintStringField("min_sdk_version", "The minimum version of the sdk that the compiled artifacts will run against For native modules, the property needs to be set when a module is a part of mainline modules(APEX). Forwarded to generated java/native module."),
            BlueprintStringField("srcs_available", "Determines whether the generated source files are available or not. When set to true, the source files can be added to `srcs` property via `:<ifacename>-<backend>-source`, e.g., \":myaidl-java-source\""),
            BlueprintObjectField("vndk", "", listOf(
                BlueprintBooleanField("enabled", "declared as a VNDK or VNDK-SP module. The vendor variant will be installed in /system instead of /vendor partition. `vendor_available` and `product_available` must be explicitly set to either true or false together with `vndk: {enabled: true}`."),
                BlueprintBooleanField("support_system_process", "declared as a VNDK-SP module, which is a subset of VNDK. `vndk: { enabled: true }` must set together. All these modules are allowed to link to VNDK-SP or LL-NDK modules only. Other dependency will cause link-type errors. If `support_system_process` is not set or set to false, the module is VNDK-core and can link to other VNDK-core, VNDK-SP or LL-NDK modules only."),
                BlueprintBooleanField("private", "declared as a VNDK-private module. This module still creates the vendor and product variants refering to the `vendor_available: true` and `product_available: true` properties. However, it is only available to the other VNDK modules but not to the non-VNDK vendor or product modules."),
                BlueprintStringField("extends", "Extending another module"),
            )),
        )),
        BlueprintObjectField("java", "Backend of the compiler generating code for Java clients. When enabled, this creates a target called \"<name>-java\".", listOf(
            BlueprintStringField("sdk_version", "Set to the version of the sdk to compile against Default: system_current"),
            BlueprintBooleanField("platform_apis", "Whether to compile against platform APIs instead of an SDK."),
            BlueprintBooleanField("enabled", "Whether to generate code in the corresponding backend. Default: true"),
            BlueprintStringListField("apex_available", ""),
            BlueprintStringField("min_sdk_version", "The minimum version of the sdk that the compiled artifacts will run against For native modules, the property needs to be set when a module is a part of mainline modules(APEX). Forwarded to generated java/native module."),
            BlueprintBooleanField("srcs_available", "Determines whether the generated source files are available or not. When set to true, the source files can be added to `srcs` property via `:<ifacename>-<backend>-source`, e.g., \":myaidl-java-source\""),
        )),
        BlueprintObjectField("ndk","Backend of the compiler generating code for C++ clients using libbinder_ndk (stable C interface to system's libbinder) When enabled, this creates a target called \"<name>-ndk\" (for apps) and \"<name>-ndk_platform\" (for platform usage).", listOf(
            BlueprintBooleanField("apps_enabled", "Currently, all ndk-supported interfaces generate two variants: - ndk - for apps to use, against an NDK - ndk_platform - for the platform to use This adds an option to disable the 'ndk' variant in cases where APIs only available in the platform version work."),
            BlueprintBooleanField("gen_log", "Whether to generate additional code for gathering information about the transactions. Default: false"),
            BlueprintBooleanField("enabled", "Whether to generate code in the corresponding backend. Default: true"),
            BlueprintStringListField("apex_available", ""),
            BlueprintStringField("min_sdk_version", "The minimum version of the sdk that the compiled artifacts will run against For native modules, the property needs to be set when a module is a part of mainline modules(APEX). Forwarded to generated java/native module."),
            BlueprintBooleanField("srcs_available", "Determines whether the generated source files are available or not. When set to true, the source files can be added to `srcs` property via `:<ifacename>-<backend>-source`, e.g., \":myaidl-java-source\""),
            BlueprintObjectField("vndk", "", listOf(
                BlueprintBooleanField("enabled", "declared as a VNDK or VNDK-SP module. The vendor variant will be installed in /system instead of /vendor partition. `vendor_available` and `product_available` must be explicitly set to either true or false together with `vndk: {enabled: true}`."),
                BlueprintBooleanField("support_system_process", "declared as a VNDK-SP module, which is a subset of VNDK. `vndk: { enabled: true }` must set together. All these modules are allowed to link to VNDK-SP or LL-NDK modules only. Other dependency will cause link-type errors. If `support_system_process` is not set or set to false, the module is VNDK-core and can link to other VNDK-core, VNDK-SP or LL-NDK modules only."),
                BlueprintBooleanField("private", "declared as a VNDK-private module. This module still creates the vendor and product variants refering to the `vendor_available: true` and `product_available: true` properties. However, it is only available to the other VNDK modules but not to the non-VNDK vendor or product modules."),
                BlueprintStringField("extends", "Extending another module"),
            )),
        )),
        BlueprintObjectField("rust","Backend of the compiler generating code for Rust clients. When enabled, this creates a target called \"<name>-rust\".", listOf(
            BlueprintBooleanField("enabled", "Whether to generate code in the corresponding backend. Default: true"),
            BlueprintStringListField("apex_available", ""),
            BlueprintStringField("min_sdk_version", "The minimum version of the sdk that the compiled artifacts will run against For native modules, the property needs to be set when a module is a part of mainline modules(APEX). Forwarded to generated java/native module."),
            BlueprintBooleanField("srcs_available", "Determines whether the generated source files are available or not. When set to true, the source files can be added to `srcs` property via `:<ifacename>-<backend>-source`, e.g., \":myaidl-java-source\""),
        )),
    )),
    BlueprintStringField("compile_multilib", "control whether this module compiles for 32-bit, 64-bit, or both. Possible values are \"32\" (compile for 32-bit only), \"64\" (compile for 64-bit only), \"both\" (compile for both architectures), or \"first\" (compile for 64-bit on a 64-bit platform, and 32-bit on a 32-bit platform)."),
    BlueprintBooleanField("debug_ramdisk", "Whether this module is installed to debug ramdisk"),
    BlueprintBooleanField("device_specific", "whether this module is specific to a device, not only for SoC, but also for off-chip peripherals. When set to true, it is installed into /odm (or /vendor/odm if odm partition does not exist, or /system/vendor/odm if both odm and vendor partitions do not exist). This implies `soc_specific:true`."),
    BlueprintObjectField("dist", "configuration to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)", listOf(
        BlueprintStringListField("targets", "Copy the output of this module to the \$DIST_DIR when `dist` is specified on the command line and any of these targets are also on the command line, or otherwise built"),
        BlueprintStringField("dest", "The name of the output artifact. This defaults to the basename of the output of the module."),
        BlueprintStringField("dir", "The directory within the dist directory to store the artifact. Defaults to the top level directory (\"\")."),
        BlueprintStringField("suffix", "A suffix to add to the artifact file name (before any extension)."),
        BlueprintStringField("tag", "A string tag to select the OutputFiles associated with the tag. If no tag is specified then it will select the default dist paths provided by the module type. If a tag of \"\" is specified then it will return the default output files provided by the modules, i.e. the result of calling OutputFiles(\"\")."),
    )),
    BlueprintStringListField("dists", "a list of configurations to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)"),
    BlueprintBooleanField("double_loadable", "Whether the library can be loaded multiple times into the same process"),
    BlueprintObjectField("dumpapi", "--dumpapi options", listOf(
        BlueprintBooleanField("no_license", "Dumps without license header (assuming it is the first comment in .aidl file). Default: false"),
    )),
    BlueprintBooleanField("enabled", "emit build rules for this module Disabling a module should only be done for those modules that cannot be built in the current environment. Modules that can build in the current environment but are not usually required (e.g. superceded by a prebuilt) should not be disabled as that will prevent them from being built by the checkbuild target and so prevent early detection of changes that have broken those modules."),
    BlueprintStringListField("flags", "Optional flags to be passed to the AIDL compiler. e.g. \"-Weverything\""),
    BlueprintBooleanField("gen_trace", "Whether tracing should be added to the interface."),
    BlueprintStringListField("host_required", "names of other modules to install on host if this module is installed"),
    BlueprintStringListField("imports", "List of aidl_interface modules that this uses. If one of your AIDL interfaces uses an interface or parcelable from another aidl_interface, you should put its name here. It could be an aidl_interface solely or with version(such as -V1)"),
    BlueprintStringListField("include_dirs", "Top level directories for includes. TODO(b/128940869): remove it if aidl_interface can depend on framework.aidl"),
    BlueprintStringListField("init_rc", "init.rc files to be installed if this module is installed"),
    BlueprintStringListField("licenses", "Describes the licenses applicable to this module. Must reference license modules."),
    BlueprintStringField("local_include_dir", "Relative path for includes. By default assumes AIDL path is relative to current directory."),
    BlueprintBooleanField("native_bridge_supported", "Whether this module is built for non-native architectures (also known as native bridge binary)"),
    BlueprintStringField("notice", "relative path to a file to include in the list of notices for the device"),
    BlueprintBooleanField("odm_available", "Whether the library can be installed on the odm image."),
    BlueprintStringField("owner", "vendor who owns this module"),
    BlueprintBooleanField("product_available", "Whether the library can be installed on the product image."),
    BlueprintBooleanField("product_specific", "whether this module is specific to a software configuration of a product (e.g. country, network operator, etc). When set to true, it is installed into /product (or /system/product if product partition does not exist)."),
    BlueprintBooleanField("proprietary", "whether this is a proprietary vendor module, and should be installed into /vendor"),
    BlueprintBooleanField("ramdisk", "Whether this module is installed to ramdisk"),
    BlueprintBooleanField("recovery", "Whether this module is installed to recovery partition"),
    BlueprintLibrariesField("required", "names of other modules to install if this module is installed"),
    BlueprintBooleanField("soc_specific", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist)."),
    BlueprintStringField("stability", "Stability promise. Currently only supports \"vintf\". If this is unset, this corresponds to an interface with stability within this compilation context (so an interface loaded here can only be used with things compiled together, e.g. on the system.img). If this is set to \"vintf\", this corresponds to a stability promise: the interface must be kept stable as long as it is used."),
    BlueprintBooleanField("system_ext_specific", "whether this module extends system. When set to true, it is installed into /system_ext (or /system/system_ext if system_ext partition does not exist)."),
    BlueprintObjectField("target", "", listOf(
        BlueprintObjectField("host", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("android", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
    )),
    BlueprintStringListField("target_required", "names of other modules to install on target if this module is installed"),
    BlueprintBooleanField("unstable", "Marks that this interface does not need to be stable. When set to true, the build system doesn't create the API dump and require it to be updated. Default is false."),
    BlueprintBooleanField("vendor", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist). Use `soc_specific` instead for better meaning."),
    BlueprintBooleanField("vendor_available", "Whether the library can be installed on the vendor image."),
    BlueprintBooleanField("vendor_ramdisk", "Whether this module is installed to vendor ramdisk"),
    BlueprintStringListField("versions", "Previous API versions that are now frozen. The version that is last in the list is considered as the most recent version."),
    BlueprintStringListField("vintf_fragments", "VINTF manifest fragments to be installed if this module is installed"),
    BlueprintStringListField("visibility", "Controls the visibility of this module to other modules."),
    BlueprintObjectField("vndk", "", listOf(
        BlueprintBooleanField("enabled", "declared as a VNDK or VNDK-SP module. The vendor variant will be installed in /system instead of /vendor partition. `vendor_available` and `product_available` must be explicitly set to either true or false together with `vndk: {enabled: true}`."),
        BlueprintBooleanField("support_system_process", "declared as a VNDK-SP module, which is a subset of VNDK. `vndk: { enabled: true }` must set together. All these modules are allowed to link to VNDK-SP or LL-NDK modules only. Other dependency will cause link-type errors. If `support_system_process` is not set or set to false, the module is VNDK-core and can link to other VNDK-core, VNDK-SP or LL-NDK modules only."),
        BlueprintBooleanField("private", "declared as a VNDK-private module. This module still creates the vendor and product variants refering to the `vendor_available: true` and `product_available: true` properties. However, it is only available to the other VNDK modules but not to the non-VNDK vendor or product modules."),
        BlueprintStringField("extends", "Extending another module")
    )),
    BlueprintStringField("vndk_use_version", "How to interpret VNDK options. We only want one library in the VNDK (not multiple versions, since this would be a waste of space/unclear, and ultimately we want all code in a given release to be updated to use a specific version). By default, this puts either the latest stable version of the library or, if there is no stable version, the unstable version of the library in the VNDK. When using this field, explicitly set it to one of the values in the 'versions' field to put that version in the VNDK or set it to the next version (1 higher than this) to mean the version that will be frozen in the next update.")
)