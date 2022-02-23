/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.module.sdk

import com.android.tools.idea.sdk.AndroidSdks
import com.github.pvoid.androidbp.toFileSystemUrl
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jdom.Element
import org.jetbrains.android.sdk.AndroidSdkAdditionalData
import org.jetbrains.android.sdk.AndroidSdkData
import java.io.File

private const val ELEMENT_BLUEPRINTS = "blueprints"
private const val ELEMENT_BLUEPRINT = "blueprint"
private const val ATTRIBUTE_BLUEPRINT_NAME = "name"
private const val ATTRIBUTE_BLUEPRINT_FILE = "file"
private const val ATTRIBUTE_ANDROID_SDK_PATH = "androidSdkPath"
private const val ATTRIBUTE_ANDROID_SDK_TARGET = "androidSdkTarget"
private const val ATTRIBUTE_PLATFORM_VERSION = "platformVersion"
private const val ATTRIBUTE_DATA_VERSION = "dataVersion"

private const val CURRENT_DATA_VERSION = 3

class AospSdkData(
    sdk: Sdk,
    androidSdk: Sdk,
    val projects: Map<String, String>,
    val platformVersion: Int,
    val dataVersion: Int = CURRENT_DATA_VERSION
) : AndroidSdkAdditionalData(androidSdk, sdk) {

    private val mAndroidSdkTarget: String? = (androidSdk.sdkAdditionalData as? AndroidSdkAdditionalData)?.buildTargetHashString
    private val mAndroidSdkPath: String? = androidSdk.homePath

    val androidSdkData: AndroidSdkData?
        get() = if (mAndroidSdkPath != null) {
            AndroidSdkData.getSdkData(mAndroidSdkPath)
        } else {
            null
        }

    init {
        buildTargetHashString = mAndroidSdkTarget // TODO: Change to current SDK label from AOSP
    }

    override fun save(element: Element) {
        Element(ELEMENT_BLUEPRINTS).also { blueprints ->
            projects.forEach { blueprint ->
                Element(ELEMENT_BLUEPRINT).apply {
                    setAttribute(ATTRIBUTE_BLUEPRINT_NAME, blueprint.key)
                    setAttribute(ATTRIBUTE_BLUEPRINT_FILE, blueprint.value)
                }.apply(blueprints::addContent)
            }
        }.also(element::addContent)

        mAndroidSdkTarget?.let {
            element.setAttribute(ATTRIBUTE_ANDROID_SDK_TARGET, it)
        }
        element.setAttribute(ATTRIBUTE_ANDROID_SDK_PATH, mAndroidSdkPath)
        element.setAttribute(ATTRIBUTE_PLATFORM_VERSION, platformVersion.toString())
        element.setAttribute(ATTRIBUTE_DATA_VERSION, dataVersion.toString())

        super.save(element)
    }

    fun getBlueprintFile(name: String): VirtualFile? {
        val path = projects[name] ?: return null
        val file = File(path)

        if (!file.exists()) {
            return null
        }

        return VirtualFileManager.getInstance().findFileByUrl(file.toFileSystemUrl())
    }

    fun isOld() = dataVersion < CURRENT_DATA_VERSION

    companion object {
        fun read(sdk: Sdk, element: Element): ((SdkModificator) -> Unit)? {
            val androidSdkPath = element.getAttribute(ATTRIBUTE_ANDROID_SDK_PATH)?.value ?: return null
            val platformVersion = try {
                element.getAttribute(ATTRIBUTE_PLATFORM_VERSION)?.intValue
            } catch (e: NumberFormatException) {
                null
            } ?: 0

            val dataVersion = try {
                element.getAttribute(ATTRIBUTE_DATA_VERSION)?.intValue
            } catch (e: NumberFormatException) {
                null
            } ?: 0

            val projects = element.getChild(ELEMENT_BLUEPRINTS)?.children
                ?.filter { it.name == ELEMENT_BLUEPRINT }?.mapNotNull { blueprint ->
                    val name = blueprint.getAttribute(ATTRIBUTE_BLUEPRINT_NAME)?.value
                    val file = blueprint.getAttribute(ATTRIBUTE_BLUEPRINT_FILE)?.value

                    if (name != null && file != null) {
                        Pair(name, file)
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()

            return { modification ->
                // TODO: Select by the build target as well
                AndroidSdks.getInstance().allAndroidSdks.firstOrNull { it.homePath == androidSdkPath }?.let {
                    modification.sdkAdditionalData = AospSdkData(sdk, it, projects, platformVersion, dataVersion)
                    modification.commitChanges()
                }
            }
        }
    }
}