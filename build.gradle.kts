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
val androidPluginVersion = "251.25410.109"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    intellijPlatform {
        intellijIdeaCommunity("2025.1.1.1")
        bundledPlugin("com.intellij.java")
        plugin("org.jetbrains.android", androidPluginVersion)
        plugin("com.android.tools.design", androidPluginVersion)
    }
}

tasks {
    // Set the JVM compatibility versions
//    withType<JavaCompile> {
//        sourceCompatibility = "17"
//        targetCompatibility = "17"
//    }
//    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions.jvmTarget = "17"
//    }

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
