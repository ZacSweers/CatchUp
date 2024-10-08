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
plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.kapt) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.foundry.root)
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.doctor) apply false
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
  alias(libs.plugins.dependencyAnalysis) apply false
  alias(libs.plugins.kotlin.plugin.compose) apply false
}

buildscript {
  dependencies {
    // Necessary for sqldelight's DB migration verification task
    classpath(libs.sqlite.xerial)
  }
}

val useProjectIsolation =
  System.getProperty("org.gradle.unsafe.isolated-projects", "false").toBoolean()

if (!useProjectIsolation) {
  apply(plugin = libs.plugins.doctor.get().pluginId)
  apply(plugin = libs.plugins.spotless.get().pluginId)
}

skippy {
  mergeOutputs = true
  global {
    applyDefaults()
    // Glob patterns of files to include in computing
    includePatterns.addAll("**/schemas/**", "app/proguard-rules.pro", "**/src/**/graphql/**")
    // Glob patterns of files that, if changed, should result in not skipping anything in the build
    neverSkipPatterns.addAll(
      ".github/workflows/**",
      "spotless/**",
      "scripts/github/schema.json",
      "config/lint/lint.xml",
    )
  }
}

if (!useProjectIsolation) {
  // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1111
  //  apply(plugin = libs.plugins.dependencyAnalysis.get().pluginId)
  //  configure<DependencyAnalysisExtension> {
  //    structure {
  //      bundle("compose-ui") {
  //        primary("androidx.compose.ui:ui")
  //        includeGroup("androidx.compose.ui")
  //        // TODO exclude ui-tooling
  //      }
  //    }
  //  }
}