/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

val PREBUILD_ETC_FIELDS = listOf(
    BlueprintStringField("name", "The name of the module. Must be unique across all modules."),
    BlueprintStringField("src", "Source file of this prebuilt. Can reference a genrule type module with the \":module\" syntax."),
    BlueprintInterfaceField("arch", ""),
    BlueprintStringField("compile_multilib", "control whether this module compiles for 32-bit, 64-bit, or both. Possible values are \"32\" (compile for 32-bit only), \"64\" (compile for 64-bit only), \"both\" (compile for both architectures), or \"first\" (compile for 64-bit on a 64-bit platform, and 32-bit on a 32-bit platform)."),
    BlueprintBooleanField("debug_ramdisk", "Whether this module is installed to debug ramdisk"),
    BlueprintBooleanField("debug_ramdisk_available", "Make this module available when building for debug ramdisk. On device without a dedicated recovery partition, the module is only available after switching root into /first_stage_ramdisk. To expose the module before switching root, install the recovery variant instead."),
    BlueprintBooleanField("device_specific", "whether this module is specific to a device, not only for SoC, but also for off-chip peripherals. When set to true, it is installed into /odm (or /vendor/odm if odm partition does not exist, or /system/vendor/odm if both odm and vendor partitions do not exist). This implies `soc_specific:true`."),
    BlueprintObjectField("dist", "configuration to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)", listOf(
        BlueprintStringListField("targets", "Copy the output of this module to the \$DIST_DIR when `dist` is specified on the command line and any of these targets are also on the command line, or otherwise built"),
        BlueprintStringField("dest", "The name of the output artifact. This defaults to the basename of the output of the module."),
        BlueprintStringField("dir", "The directory within the dist directory to store the artifact. Defaults to the top level directory (\"\")."),
        BlueprintStringField("suffix", "A suffix to add to the artifact file name (before any extension)."),
        BlueprintStringField("tag", "A string tag to select the OutputFiles associated with the tag. If no tag is specified then it will select the default dist paths provided by the module type. If a tag of \"\" is specified then it will return the default output files provided by the modules, i.e. the result of calling OutputFiles(\"\")."),
    )),
    BlueprintStringListField("dists", "a list of configurations to distribute output files from this module to the distribution directory (default: \$OUT/dist, configurable with \$DIST_DIR)"),
    BlueprintBooleanField("enabled", "emit build rules for this module Disabling a module should only be done for those modules that cannot be built in the current environment. Modules that can build in the current environment but are not usually required (e.g. superceded by a prebuilt) should not be disabled as that will prevent them from being built by the checkbuild target and so prevent early detection of changes that have broken those modules."),
    BlueprintStringField("filename", "Optional name for the installed file. If unspecified, name of the module is used as the file name."),
    BlueprintBooleanField("filename_from_src", "When set to true, and filename property is not set, the name for the installed file is the same as the file name of the source file."),
    BlueprintStringListField("host_required", "names of other modules to install on host if this module is installed"),
    BlueprintStringListField("init_rc", "init.rc files to be installed if this module is installed"),
    BlueprintBooleanField("installable", "Whether this module is directly installable to one of the partitions. Default: true."),
    BlueprintStringListField("licenses", "Describes the licenses applicable to this module. Must reference license modules."),
    BlueprintInterfaceField("multilib",""),
    BlueprintBooleanField("native_bridge_supported", "Whether this module is built for non-native architectures (also known as native bridge binary)"),
    BlueprintStringField("notice", "relative path to a file to include in the list of notices for the device"),
    BlueprintStringField("owner", "vendor who owns this module"),
    BlueprintBooleanField("product_specific", "whether this module is specific to a software configuration of a product (e.g. country, network operator, etc). When set to true, it is installed into /product (or /system/product if product partition does not exist)."),
    BlueprintBooleanField("proprietary", "whether this is a proprietary vendor module, and should be installed into /vendor"),
    BlueprintBooleanField("ramdisk", "Whether this module is installed to ramdisk"),
    BlueprintBooleanField("ramdisk_available", "Make this module available when building for ramdisk. On device without a dedicated recovery partition, the module is only available after switching root into /first_stage_ramdisk. To expose the module before switching root, install the recovery variant instead."),
    BlueprintBooleanField("recovery", "Whether this module is installed to recovery partition"),
    BlueprintBooleanField("recovery_available", "Make this module available when building for recovery."),
    BlueprintStringField("relative_install_path", "Optional subdirectory under which this file is installed into, cannot be specified with sub_dir."),
    BlueprintStringListField("required", "names of other modules to install if this module is installed"),
    BlueprintBooleanField("soc_specific", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist)."),
    BlueprintStringField("sub_dir", "Optional subdirectory under which this file is installed into, cannot be specified with relative_install_path, prefer relative_install_path."),
    BlueprintStringListField("symlinks", "Install symlinks to the installed file."),
    BlueprintBooleanField("system_ext_specific", "whether this module extends system. When set to true, it is installed into /system_ext (or /system/system_ext if system_ext partition does not exist)."),
    BlueprintInterfaceField("target", ""),
    BlueprintObjectField("target", "", listOf(
        BlueprintObjectField("host", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("android", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
    )),
    BlueprintStringListField("target_required", "names of other modules to install on target if this module is installed"),
    BlueprintBooleanField("vendor", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist). Use `soc_specific` instead for better meaning."),
    BlueprintBooleanField("vendor_ramdisk", "Whether this module is installed to vendor ramdisk"),
    BlueprintBooleanField("vendor_ramdisk_available", "Make this module available when building for vendor ramdisk. On device without a dedicated recovery partition, the module is only available after switching root into /first_stage_ramdisk. To expose the module before switching root, install the recovery variant instead."),
    BlueprintStringListField("vintf_fragments", "VINTF manifest fragments to be installed if this module is installed"),
    BlueprintStringListField("visibility", "Controls the visibility of this module to other modules."),
)