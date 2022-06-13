/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.devtools.ksp.gradle.KspExtension
import deps.versions

buildscript {
  repositories {
    google()
    mavenCentral()
    maven(deps.build.repositories.plugins)
    maven(deps.build.repositories.snapshots)
    maven(deps.build.repositories.androidxSnapshots)
    maven(deps.build.repositories.kotlinDev)
    maven("https://storage.googleapis.com/r8-releases/raw")
  }

  dependencies {
    classpath(deps.android.gradlePlugin)
    classpath(deps.kotlin.gradlePlugin)
    classpath(deps.kotlin.noArgGradlePlugin)
    classpath(deps.android.firebase.gradlePlugin)
    classpath(deps.build.gradlePlugins.bugsnag)
    classpath(deps.apollo.gradlePlugin)
    classpath(deps.build.gradlePlugins.playPublisher)
    classpath(deps.build.gradlePlugins.spotless)
    classpath(deps.build.gradlePlugins.redacted)
    classpath(deps.dagger.hilt.gradlePlugin)
    classpath("com.squareup.anvil:gradle-plugin:${deps.anvil.version}")
    classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${deps.ksp.version}")
    classpath("dev.zacsweers.moshix:moshi-gradle-plugin:${deps.moshi.moshix.VERSION}")
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.42.0"
  id("catchup")
  id("com.osacky.doctor") version "0.8.1"
}

doctor {
  // G1 is faster now
  warnWhenNotUsingParallelGC.set(false)
  javaHome {
    ensureJavaHomeMatches.set(false)
  }
}

apply {
  from(rootProject.file("gradle/spotless-config.gradle"))
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven(deps.build.repositories.jitpack)
    maven(deps.build.repositories.snapshots)
    maven(deps.build.repositories.androidxSnapshots)
    maven(deps.build.repositories.kotlinDev)
    // Pre-release artifacts of compose-compiler, used to test with future Kotlin versions
    // https://androidx.dev/storage/compose-compiler/repository
    maven("https://androidx.dev/storage/compose-compiler/repository/") {
      name = "compose-compiler"
      content {
        // this repository *only* contains compose-compiler artifacts
        includeGroup("androidx.compose.compiler")
      }
    }
  }

  configurations.configureEach {
    resolutionStrategy.eachDependency {
      when {
        requested.name.startsWith("kotlin-stdlib") -> {
          useTarget(
            "${requested.group}:${requested.name.replace("jre", "jdk")}:${requested.version}"
          )
        }
        else -> when (requested.group) {
          "com.android.support" -> {
            if ("multidex" !in requested.name) {
              useVersion(versions.legacySupport)
            }
          }
          "org.jetbrains.kotlin" -> useVersion(versions.kotlin)
          "com.google.dagger" -> useVersion(versions.dagger)
        }
      }
    }
  }

  pluginManager.withPlugin(deps.ksp.pluginId) {
    configure<KspExtension> {
      blockOtherCompilerPlugins = true
    }
  }
}
