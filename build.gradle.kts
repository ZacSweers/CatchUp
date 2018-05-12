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

/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import deps
import deps.versions
import org.gradle.initialization.StartParameterBuildOptions.BuildScanOption
import org.gradle.internal.scan.config.BuildScanConfig

buildscript {
  repositories {
    google()
    jcenter()
    maven { url = uri(deps.build.repositories.plugins) }
    maven { url = uri(deps.build.repositories.kotlineap) }
    maven { url = uri(deps.build.repositories.snapshots) }
  }

  dependencies {
    // Seemingly random other classpath dependencies are because of a gradle bug when sharing
    // buildscript deps. Oddly, not only do all of these need to be here to match :app, but they
    // need to also be in the same order.
    classpath(deps.android.gradlePlugin)
    classpath(deps.kotlin.gradlePlugin)
    classpath(deps.kotlin.noArgGradlePlugin)
    classpath(deps.android.firebase.gradlePlugin)
    classpath(deps.build.gradlePlugins.bugsnag)
    classpath(deps.build.gradlePlugins.psync)
    classpath(deps.errorProne.gradlePlugin)
    classpath(deps.apollo.gradlePlugin)
    classpath(deps.build.gradlePlugins.playPublisher)
  }
}

plugins {
  id("com.gradle.build-scan") version "1.13.2"
  id("com.github.ben-manes.versions") version "0.17.0"
}

buildScan {
  setTermsOfServiceAgree("yes")
  setTermsOfServiceUrl("https://gradle.com/terms-of-service")
}

// Due to https://github.com/gradle/gradle/issues/4823
// Breaks configure on demand
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {

  repositories {
    google()
    jcenter()
    maven { url = uri(deps.build.repositories.jitpack) }
    maven { url = uri(deps.build.repositories.snapshots) }
    maven { url = uri("https://oss.jfrog.org/libs-snapshot") }
  }

  configurations.all {
    resolutionStrategy.eachDependency {
      when {
        requested.name.startsWith("kotlin-stdlib") -> {
          useTarget(
              "${requested.group}:${requested.name.replace("jre", "jdk")}:${requested.version}")
        }
        else -> when (requested.group) {
        // We want to force all support libraries to use the same version, even if they"re transitive.
          "com.android.support" -> {
            if ("multidex" !in requested.name) {
              useVersion(versions.jetpack)
            }
          }
        // We want to force all play services libraries to use the same version, even if they"re transitive.
          "com.google.android.gms" -> useVersion(versions.playServices)
        // We want to force all play services libraries to use the same version, even if they"re transitive.
          "com.google.firebase" -> useVersion(versions.firebase)
        // We want to force all kotlin libraries to use the same version, even if they"re transitive.
          "org.jetbrains.kotlin" -> useVersion(versions.kotlin)
        }
      }
    }
  }
}

tasks {
  "wrapper"(Wrapper::class) {
    gradleVersion = "4.7"
    distributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
  }
}
