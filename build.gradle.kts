/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.21"
}

group = "com.github.pvoid.androidbp.next"
version = "1.0.0-RC15"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    intellijPlatform {
        intellijIdeaCommunity("2025.2.4")
        bundledPlugin("com.intellij.java")
        plugin("org.jetbrains.android", "252.27397.103")
        plugin("com.android.tools.design", "252.27397.28")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("251")
        changeNotes.set("""
        <ul>
            <li>Add IDEA 2024.2.1 support</li>
        </ul>
        """.trimIndent())

    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}
