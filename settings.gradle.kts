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
import com.dropbox.focus.FocusExtension

pluginManagement {
  // Non-delegate APIs are annoyingly not public so we have to use withGroovyBuilder
  fun hasProperty(key: String): Boolean {
    return settings.withGroovyBuilder { "hasProperty"(key) as Boolean }
  }

  fun findProperty(key: String): String? {
    return if (hasProperty(key)) {
      settings.withGroovyBuilder { "getProperty"(key) as String }
    } else {
      null
    }
  }

  repositories {
    // Snapshots
    if (hasProperty("foundry.gradle.config.enableSnapshots")) {
      maven(findProperty("catchup.mavenUrls.snapshots.sonatype")!!) {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("catchup.mavenUrls.snapshots.sonatypes01")!!) {
        name = "snapshots-maven-central-s01"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("catchup.mavenUrls.snapshots.androidx")!!) {
        name = "snapshots-androidx"
        mavenContent { snapshotsOnly() }
        content { includeGroupByRegex("androidx.*") }
      }
    }

    // MavenLocal, used when consuming a locally-installed artifact
    if (hasProperty("catchup.config.enableMavenLocal")) {
      mavenLocal()
    }

    // Maven central
    mavenCentral()

    // Google/Firebase/GMS/Androidx libraries
    // Note that we don't use exclusiveContent for androidx libraries so that snapshots work
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("android\\.arch.*")
        includeGroupByRegex("org\\.chromium.*")
      }
    }

    // Kotlin dev (previously bootstrap) repository, useful for testing against Kotlin dev builds.
    // Usually only tested on CI shadow jobs
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1616514468003200?thread_ts=1616509748.001400&cid=C0KLZSCHF
    maven(findProperty("catchup.mavenUrls.kotlinDev")!!) {
      name = "Kotlin-Bootstrap"
      content {
        // this repository *only* contains Kotlin artifacts (don't try others here)
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }

    // Pre-release artifacts of compose-compiler, used to test with future Kotlin versions
    // https://androidx.dev/storage/compose-compiler/repository
    maven("https://androidx.dev/storage/compose-compiler/repository/") {
      name = "compose-compiler"
      content {
        // this repository *only* contains compose-compiler artifacts
        includeGroup("androidx.compose.compiler")
      }
    }

    // R8 repo for R8/D8 releases
    exclusiveContent {
      forRepository {
        maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
      }
      filter { includeModule("com.android.tools", "r8") }
    }

    // JB Compose Repo
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose-JB" }

    // For Gradle plugins only. Last because this proxies to jcenter >_>
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    if (System.getenv("DEP_OVERRIDES") == "true") {
      val overrides = System.getenv().filterKeys { it.startsWith("DEP_OVERRIDE_") }
      for (catalog in this) {
        for ((key, value) in overrides) {
          // Case-sensitive, don't adjust it after removing the prefix!
          val catalogKey = key.removePrefix("DEP_OVERRIDE_")
          println("Overriding $catalogKey with $value")
          catalog.version(catalogKey, value)
        }
      }
    }
  }

  // Always use repositories we define here and fail if any per-project plugin attempts to source
  // from their own. We want to keep this a (mostly) hermetic environment and keep repo declarations
  // consolidated in one place!
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

  // Non-delegate APIs are annoyingly not public so we have to use withGroovyBuilder
  fun hasProperty(key: String): Boolean {
    return settings.withGroovyBuilder { "hasProperty"(key) as Boolean }
  }

  fun findProperty(key: String): String? {
    return if (hasProperty(key)) {
      settings.withGroovyBuilder { "getProperty"(key) as String }
    } else {
      null
    }
  }

  repositories {
    // Repos are declared roughly in order of likely to hit.

    // Snapshots/local go first in order to preempt other repos that may contain unscrupulous
    // snapshot artifacts

    // Snapshots
    if (hasProperty("foundry.gradle.config.enableSnapshots")) {
      maven(findProperty("catchup.mavenUrls.snapshots.sonatype")!!) {
        name = "snapshots-maven-central"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("catchup.mavenUrls.snapshots.sonatypes01")!!) {
        name = "snapshots-maven-central-s01"
        mavenContent { snapshotsOnly() }
      }
      maven(findProperty("catchup.mavenUrls.snapshots.androidx")!!) {
        name = "snapshots-androidx"
        mavenContent { snapshotsOnly() }
        content { includeGroupByRegex("androidx.*") }
      }
    }

    // MavenLocal, used when consuming a locally-installed artifact
    if (hasProperty("catchup.config.enableMavenLocal")) {
      mavenLocal()
    }

    mavenCentral()

    // Google/Firebase/GMS/Androidx libraries
    // Note that we don't use exclusiveContent for androidx libraries so that snapshots work
    google {
      content {
        includeGroupByRegex("androidx.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("android\\.arch.*")
        includeGroupByRegex("org\\.chromium.*")
        includeModule("com.google.android.flexbox", "flexbox")
      }
    }

    // Kotlin dev (previously bootstrap) repository, useful for testing against Kotlin dev builds.
    // Usually only tested on CI shadow jobs
    // https://kotlinlang.slack.com/archives/C0KLZSCHF/p1616514468003200?thread_ts=1616509748.001400&cid=C0KLZSCHF
    maven(findProperty("catchup.mavenUrls.kotlinDev")!!) {
      name = "Kotlin-Bootstrap"
      content {
        // this repository *only* contains Kotlin artifacts (don't try others here)
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }

    // Pre-release artifacts of compose-compiler, used to test with future Kotlin versions
    // https://androidx.dev/storage/compose-compiler/repository
    maven("https://androidx.dev/storage/compose-compiler/repository/") {
      name = "compose-compiler"
      content {
        // this repository *only* contains compose-compiler artifacts
        includeGroup("androidx.compose.compiler")
      }
    }

    // R8 repo for R8/D8 releases
    exclusiveContent {
      forRepository {
        maven("https://storage.googleapis.com/r8-releases/raw") { name = "R8-releases" }
      }
      filter { includeModule("com.android.tools", "r8") }
    }

    // JB Compose Repo
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { name = "Compose-JB" }
  }
}

plugins {
  id("com.gradle.develocity") version "3.19.1"
  id("com.dropbox.focus") version "0.7.0" apply false
}

val useProjectIsolation =
  System.getProperty("org.gradle.unsafe.isolated-projects", "false").toBoolean()
val focusDisabled = System.getenv("NO_FOCUS").toBoolean()

if (focusDisabled || useProjectIsolation) {
  apply(from = "settings-all.gradle.kts")
} else {
  apply(plugin = "com.dropbox.focus")
  configure<FocusExtension> { allSettingsFileName.set("settings-all.gradle.kts") }
}

develocity {
  buildScan {
    publishing {
      onlyIf { true }
      termsOfUseAgree = "yes"
      termsOfUseUrl = "https://gradle.com/terms-of-service"
    }

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
    obfuscation {
      username { "redacted" }
      hostname { "redacted" }
      ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
    }
  }
}

rootProject.name = "CatchUp"

inline fun configureIncludedBuild(key: String, body: (path: String) -> Unit) {
  System.getProperty("catchup.include-build.$key")?.let(body)
}

// See comments on systemProp.catchup.include-build.foundry property in gradle.properties
configureIncludedBuild("foundry") { path ->
  println("including build $path")
  includeBuild(path) {
    dependencySubstitution {
      substitute(module("com.slack.foundry:gradle-plugin"))
        .using(project(":platforms:gradle:foundry-gradle-plugin"))
      substitute(module("com.slack.foundry:agp-handler-api"))
        .using(project(":platforms:gradle:agp-handlers:agp-handler-api"))
      substitute(module("com.slack.foundry:foundry-common"))
        .using(project(":tools:foundry-common"))
      substitute(module("com.slack.foundry:skippy"))
        .using(project(":tools:skippy"))
      substitute(module("com.slack.foundry:tracing"))
        .using(project(":tools:tracing"))
    }
  }
}

// See comments on systemProp.catchup.include-build.dagp property in gradle.properties
configureIncludedBuild("dagp") { path ->
  includeBuild(path) {
    dependencySubstitution {
      substitute(module("com.autonomousapps:dependency-analysis-gradle-plugin")).using(project(":"))
    }
  }
}

// See comments on systemProp.catchup.include-build.anvil property in gradle.properties
configureIncludedBuild("anvil") { path ->
  includeBuild(path) {
    dependencySubstitution {
      substitute(module("com.squareup.anvil:compiler")).using(project(":compiler"))
    }
  }
}

include(":platform")

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// https://github.com/gradle/gradle/blob/9a08b2368ca049a33783781c7810a7d2f4aaeab2/subprojects/docs/src/docs/userguide/running-builds/configuration_cache.adoc#stable-configuration-cache
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
