/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
}

group = "com.github.pvoid.androidbp.next"
version = "1.0.0-RC15"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    type.set("IC")
    version.set("2024.3")

    plugins.set(listOf("java", "org.jetbrains.android:243.21565.214", "com.android.tools.design:243.21565.214"))
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("243")
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
dependencies {
    implementation(kotlin("stdlib"))
}