/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea.project

import java.io.File

data class AndroidDependencyRecord(
    val name: String,
    val packageName: String?,
    val apk: List<File>,
    val res: List<File>,
    val generatedRes: List<File>,
    val manifests: List<File>,
    val assets: List<File>,
    val R: List<File>,
    val jars: List<File>,
) {
    fun isValid(): Boolean {
        return packageName != null && manifests.isNotEmpty() && (res.isNotEmpty() || apk.isNotEmpty() && R.isNotEmpty())
    }

    class Builder(
        val name: String
    ) {
        private val apk = mutableListOf<File>()
        private var packageName: String? = null
        private val res = mutableListOf<File>()
        private val generatedRes = mutableListOf<File>()
        private val manifests = mutableListOf<File>()
        private val assets = mutableListOf<File>()
        private val R = mutableListOf<File>()
        private val jars = mutableListOf<File>()

        fun withResApk(apk: File?): Builder {
            if (apk != null) {
                this.apk.add(apk)
            }
            return this
        }

        fun withRes(res: File?): Builder {
            if (res != null) {
                this.res.add(res)
            }
            return this
        }

        fun withGeneratedRes(res: File?): Builder {
            if (res != null) {
                this.generatedRes.add(res)
            }
            return this
        }

        fun withManifest(manifest: File?): Builder {
            if (manifest != null) {
                this.manifests.add(manifest)
            }
            return this
        }

        fun withAssets(assets: File?): Builder {
            if (assets != null) {
                this.assets.add(assets)
            }
            return this
        }

        fun withR(R: File?): Builder {
            if (R != null) {
                this.R.add(R)
            }
            return this
        }

        fun withPackageName(packageName: String?): Builder {
            if (packageName != null) {
                this.packageName = packageName
            }
            return this
        }

        fun withJar(jar: File?): Builder {
            if (jar != null) {
                this.jars.add(jar)
            }
            return this
        }

        fun build(): AndroidDependencyRecord {
            return AndroidDependencyRecord(name, packageName, apk, res, generatedRes, manifests, assets, R, jars)
        }
    }
}