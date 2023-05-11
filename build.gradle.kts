import slack.gradle.avoidance.AffectedProjectsDefaults
import slack.gradle.avoidance.ComputeAffectedProjectsTask

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
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.agp.application) apply false
  alias(libs.plugins.sgp.root)
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.versions)
  alias(libs.plugins.spotless)
  alias(libs.plugins.doctor)
  alias(libs.plugins.anvil) apply false
  alias(libs.plugins.cacheFixPlugin) apply false
  //  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.kotlin.noarg) apply false
  alias(libs.plugins.moshix) apply false
  alias(libs.plugins.retry) apply false
  alias(libs.plugins.bugsnag) apply false
  alias(libs.plugins.sortDependencies) apply false
}

buildscript {
  dependencies {
    // Necessary for sqldelight's DB migration verification task
    classpath(libs.sqlite.xerial)
  }
}

subprojects {
  pluginManager.withPlugin("com.squareup.anvil") {
    dependencies { add("compileOnly", libs.anvil.annotations) }
  }
  //  pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
  //    dependencies {
  //      add("detektPlugins", libs.detekt.plugins.twitterCompose)
  //    }
  //  }
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
