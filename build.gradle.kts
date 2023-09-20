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
import slack.gradle.avoidance.AffectedProjectsDefaults
import slack.gradle.avoidance.ComputeAffectedProjectsTask

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.sgp.root)
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.spotless)
  alias(libs.plugins.doctor)
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.cacheFixPlugin) apply false
  //  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.kotlin.noarg) apply false
  alias(libs.plugins.moshix) apply false
  alias(libs.plugins.retry) apply false
  alias(libs.plugins.bugsnag) apply false
  alias(libs.plugins.sortDependencies) apply false
  alias(libs.plugins.sqldelight) apply false
  alias(libs.plugins.dependencyAnalysis)
}

buildscript {
  dependencies {
    // Necessary for sqldelight's DB migration verification task
    classpath(libs.sqlite.xerial)
  }
}

val useK2 = findProperty("kotlin.experimental.tryK2")?.toString().toBoolean()

subprojects {
  pluginManager.withPlugin("com.squareup.anvil") {
    dependencies { add("compileOnly", libs.anvil.annotations) }
  }

  // TODO remove after kotlinpoet 1.14.0 is out with https://github.com/square/kotlinpoet/pull/1568
  configurations.configureEach {
    resolutionStrategy {
      eachDependency {
        if (requested.group == "com.squareup" && requested.name.contains("kotlinpoet")) {
          useVersion("1.12.0")
        }
      }
    }
  }
}

tasks.named<ComputeAffectedProjectsTask>("computeAffectedProjects") {
  // Glob patterns of files to include in computing
  includePatterns.addAll(AffectedProjectsDefaults.DEFAULT_INCLUDE_PATTERNS)
  includePatterns.addAll(
    "**/schemas/**",
    "app/proguard-rules.pro",
    "**/src/**/graphql/**",
  )
  // Glob patterns of files that, if changed, should result in not skipping anything in the build
  neverSkipPatterns.addAll(AffectedProjectsDefaults.DEFAULT_NEVER_SKIP_PATTERNS)
  neverSkipPatterns.addAll(
    ".github/workflows/**",
    "spotless/**",
    "scripts/github/schema.json",
    "config/lint/lint.xml"
  )
}

dependencyAnalysis {
  this.dependencies {
    bundle("compose-ui") {
      primary("androidx.compose.ui:ui")
      includeGroup("androidx.compose.ui")
      // TODO exclude ui-tooling
    }
  }
}
