/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.pvoid.androidbp.idea

import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.Logger

val LOG = Logger.getInstance("#com.github.pvoid.androidbp").apply {
    setLevel(LogLevel.DEBUG)
}