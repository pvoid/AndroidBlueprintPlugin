/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp

import com.google.common.annotations.VisibleForTesting
import org.xml.sax.Attributes
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

class ManifestInfo(
    val reference: String
) {

    companion object {
        fun read(inp: InputStream, sdkRootPath: String?): ManifestInfo {
            val factory = SAXParserFactory.newInstance()
            val saxParser = factory.newSAXParser()
            val handler = ManifestInfoHandler(sdkRootPath)

            try {
                saxParser.parse(inp, handler)
            } catch (e: SAXParseException) {
                throw IOException(e)
            }

            return handler.toInfo()
        }
    }
}

@VisibleForTesting
class ManifestInfoHandler(
    private val mSdkRootPath: String?
) : DefaultHandler() {

    private var mRef: String? = null

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        if (qName == "default") {
            mRef = attributes?.getValue("revision")
        }

        if (qName == "include") {
            val manifestName = attributes?.getValue("name")
            val manifestFile = File(mSdkRootPath, ".repo/manifests/$manifestName")
            if (manifestFile.exists() && manifestFile.isFile) {
                val manifestInfo = FileInputStream(manifestFile).use {
                    try {
                        ManifestInfo.read(it, mSdkRootPath)
                    } catch (e: IOException) {
                        LOG.error(e)
                        null
                    }
                }
                if (manifestInfo != null) {
                    mRef = manifestInfo.reference
                }
            }
        }
    }

    fun toInfo(): ManifestInfo {
        val ref = mRef ?: throw IOException("Missed revision attribute or <default> node")
        return ManifestInfo(ref)
    }
}
