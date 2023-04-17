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
    val apk: List<String>,
    val res: List<String>,
    val generatedRes: List<String>,
    val manifests: List<File>,
    val assets: List<String>,
    val R: List<String>,
    val jars: List<File>,
) {
    fun isValid(): Boolean {
        return packageName != null && manifests.isNotEmpty() && (res.isNotEmpty() || apk.isNotEmpty() && R.isNotEmpty())
    }

    class Builder(
        val name: String
    ) {
        private val apk = mutableListOf<String>()
        private var packageName: String? = null
        private val res = mutableListOf<String>()
        private val generatedRes = mutableListOf<String>()
        private val manifests = mutableListOf<File>()
        private val assets = mutableListOf<String>()
        private val R = mutableListOf<String>()
        private val jars = mutableListOf<File>()

        fun withResApk(apk: String): Builder {
            this.apk.add(apk)
            return this
        }

        fun withRes(res: String): Builder {
            this.res.add(res)
            return this
        }

        fun withGeneratedRes(res: String): Builder {
            this.generatedRes.add(res)
            return this
        }

        fun withManifest(manifest: File): Builder {
            this.manifests.add(manifest)
            return this
        }

        fun withAssets(assets: String): Builder {
            this.assets.add(assets)
            return this
        }

        fun withR(R: String): Builder {
            this.R.add(R)
            return this
        }

        fun withPackageName(packageName: String): Builder {
            this.packageName = packageName
            return this
        }

        fun withJar(jar: File): Builder {
            this.jars.add(jar)
            return this
        }

        fun build(): AndroidDependencyRecord {
            return AndroidDependencyRecord(name, packageName, apk, res, generatedRes, manifests, assets, R, jars)
        }
    }
}