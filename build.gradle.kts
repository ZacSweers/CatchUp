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
  }
}

plugins {
  id("com.gradle.build-scan") version "2.4.1"
  id("com.github.ben-manes.versions") version "0.24.0"
}

buildScan {
  termsOfServiceAgree = "yes"
  termsOfServiceUrl = "https://gradle.com/terms-of-service"
}

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
        }
      }
    }
  }

  apply {
    from(rootProject.file("gradle/spotless-config.gradle"))
  }
}
