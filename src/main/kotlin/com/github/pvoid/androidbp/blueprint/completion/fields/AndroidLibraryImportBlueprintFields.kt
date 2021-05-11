/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.blueprint.completion.fields

val ANDROID_LIBRARY_IMPORT_FIELDS = listOf(
    BlueprintStringField("name", "The name of the module. Must be unique across all modules."),
    BlueprintStringListField("aars", ""),
    BlueprintStringField("compile_multilib", "control whether this module compiles for 32-bit, 64-bit, or both. Possible values are \"32\" (compile for 32-bit only), \"64\" (compile for 64-bit only), \"both\" (compile for both architectures), or \"first\" (compile for 64-bit on a 64-bit platform, and 32-bit on a 32-bit platform"),
    BlueprintStringListField("defaults", ""),
    BlueprintBooleanField("device_specific", "whether this module is specific to a device, not only for SoC, but also for off-chip peripherals. When set to true, it is installed into /odm (or /vendor/odm if odm partition does not exist, or /system/vendor/odm if both odm and vendor partitions do not exist). This implies `soc_specific:true`."),
    BlueprintObjectField("dist", "", listOf(
        BlueprintStringListField("targets", "copy the output of this module to the \$DIST_DIR when `dist` is specified on the command line and any of these targets are also on the command line, or otherwise built"),
        BlueprintStringField("dest", "The name of the output artifact. This defaults to the basename of the output of the module."),
        BlueprintStringField("dir", "The directory within the dist directory to store the artifact. Defaults to the top level directory (\"\")."),
        BlueprintStringField("suffix", "A suffix to add to the artifact file name (before any extension)."),
    )),
    BlueprintBooleanField("enabled", "emit build rules for this module"),
    BlueprintStringListField("init_rc", "init.rc files to be installed if this module is installed"),
    BlueprintBooleanField("jetifier", "if set to true, run Jetifier against .aar file. Defaults to false."),
    BlueprintLibrariesListField("libs", ""),
    BlueprintStringField("min_sdk_version", ""),
    BlueprintStringField("notice", "relative path to a file to include in the list of notices for the device"),
    BlueprintStringField("owner", "vendor who owns this module"),
    BlueprintBooleanField("prefer", "When prefer is set to true the prebuilt will be used instead of any source module with a matching name."),
    BlueprintBooleanField("product_services_specific", "whether this module provides services owned by the OS provider to the core platform. When set to true, it is installed into /product_services (or /system/product_services if product_services partition does not exist)."),
    BlueprintBooleanField("product_specific", "whether this module is specific to a software configuration of a product (e.g. country, network operator, etc). When set to true, it is installed into /product (or /system/product if product partition does not exist)."),
    BlueprintBooleanField("proprietary", "whether this is a proprietary vendor module, and should be installed into /vendor"),
    BlueprintBooleanField("recovery", "Whether this module is installed to recovery partition"),
    BlueprintLibrariesListField("required", "names of other modules to install if this module is installed"),
    BlueprintStringField("sdk_version", ""),
    BlueprintBooleanField("soc_specific", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist)."),
    BlueprintLibrariesListField("static_libs", ""),
    BlueprintObjectField("target", "", listOf(
        BlueprintObjectField("host", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
        BlueprintObjectField("android", "", listOf(
            BlueprintStringField("compile_multilib", "")
        )),
    )),
    BlueprintBooleanField("vendor", "whether this module is specific to an SoC (System-On-a-Chip). When set to true, it is installed into /vendor (or /system/vendor if vendor partition does not exist). Use `soc_specific` instead for better meaning."),
    BlueprintStringListField("vintf_fragments", "VINTF manifest fragments to be installed if this module is installed"),
)