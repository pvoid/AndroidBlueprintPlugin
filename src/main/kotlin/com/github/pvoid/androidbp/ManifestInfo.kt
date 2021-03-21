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
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class ManifestInfo(
    val reference: String
) {

    companion object {
        fun read(inp: InputStream): ManifestInfo {
            val factory = SAXParserFactory.newInstance()
            val saxParser = factory.newSAXParser()
            val handler = ManifestInfoHandler()

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
class ManifestInfoHandler : DefaultHandler() {

    private var mRef: String? = null

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        if (qName == "default") {
            mRef = attributes?.getValue("revision")
        }
    }

    fun toInfo(): ManifestInfo {
        val ref = mRef ?: throw IOException("Missed revision attribute or <default> node")
        return ManifestInfo(ref)
    }
}