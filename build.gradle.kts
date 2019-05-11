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

import deps.versions

buildscript {
  repositories {
    google()
    mavenCentral()
    jcenter()
    maven { url = uri(deps.build.repositories.kotlineap) }
    maven { url = uri(deps.build.repositories.kotlinx) }
    maven { url = uri(deps.build.repositories.plugins) }
    maven { url = uri(deps.build.repositories.snapshots) }
    maven {
      url = uri("http://storage.googleapis.com/r8-releases/raw/master")
    }
  }

  configurations.all {
    resolutionStrategy {
      force("net.sf.proguard:proguard-base:6.1.0beta2")
    }
  }

  dependencies {
    classpath("com.android.tools:r8:86133db9ec0b631f12bf6a8eebc0f81c8aef5d1e")  // Must be before the Gradle Plugin for Android.
    classpath(deps.android.gradlePlugin)
    classpath(deps.kotlin.gradlePlugin)
    classpath(deps.kotlin.noArgGradlePlugin)
    classpath(deps.android.firebase.gradlePlugin)
    classpath(deps.build.gradlePlugins.bugsnag)
    classpath(deps.apollo.gradlePlugin)
    classpath(deps.build.gradlePlugins.playPublisher)
    classpath("androidx.benchmark:benchmark-gradle-plugin:1.0.0-alpha01")
    classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.31")
  }
}

plugins {
  id("com.gradle.build-scan") version "2.3"
  id("com.github.ben-manes.versions") version "0.21.0"
}

buildScan {
  termsOfServiceAgree = "yes"
  setTermsOfServiceUrl("https://gradle.com/terms-of-service")
}

// Due to https://github.com/gradle/gradle/issues/4823
// Breaks configure on demand
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {

  repositories {
    google()
    mavenCentral()
    jcenter()
    maven { url = uri(deps.build.repositories.kotlineap) }
    maven { url = uri(deps.build.repositories.kotlinx) }
    maven { url = uri(deps.build.repositories.jitpack) }
    maven { url = uri(deps.build.repositories.snapshots) }
  }

  configurations.all {
    resolutionStrategy.eachDependency {
      when {
        requested.name.startsWith("kotlin-stdlib") -> {
          useTarget(
              "${requested.group}:${requested.name.replace("jre", "jdk")}:${requested.version}")
        }
        else -> when (requested.group) {
          "com.android.support" -> {
            if ("multidex" !in requested.name) {
              useVersion(versions.legacySupport)
            }
          }
          "org.jetbrains.kotlin" -> useVersion(versions.kotlin)
          "com.google.dagger" -> useVersion(versions.dagger)
          "com.google.errorprone" -> {
            if (requested.name in setOf("javac", "error_prone_annotations")) {
              useVersion(versions.errorProne)
            }
          }
        }
      }
    }
  }
}
