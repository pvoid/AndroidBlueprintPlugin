/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.github.pvoid.androidbp.next"
version = "1.0.0-RC13"

repositories {
    mavenCentral()
}

intellijPlatform {
    pluginConfiguration {
        name = "AndroidBluePrint"
//        changeNotes.set("""
//        <ul>
//            <li>Add IDEA 2023.3 support</li>
//        </ul>
//        """.trimIndent())
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        instrumentationTools()

        plugins("org.jetbrains.android:242.21829.142", "com.android.tools.design:242.21829.142")
        bundledPlugin("com.intellij.java")

        val type = providers.gradleProperty("platformType")
        val version = providers.gradleProperty("platformVersion")

        create(type, version)
    }

}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
//intellijPlatform {
//    type.set("IC")
//    version.set("2024.2.1")
//
//    plugins.set(listOf("java", "org.jetbrains.android:242.21829.142", "com.android.tools.design:242.21829.142"))
//    downloadSources.set(true)
//    updateSinceUntilBuild.set(true)
//}

kotlin {
    jvmToolchain(21)
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}
