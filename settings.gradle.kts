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
    if (hasProperty("catchup.config.enableSnapshots")) {
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
    // Specifically Google's maven central mirror:
    // https://maven-central.storage.googleapis.com/index.html
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central (GCP Mirror)"
    }
    // Google's mirror is sometimes slow to index new releases, so check central just in case!
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
    if (hasProperty("catchup.config.enableSnapshots")) {
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
    // Specifically Google's maven central mirror:
    // https://maven-central.storage.googleapis.com/index.html
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central (GCP Mirror)"
    }
    // Google's mirror is sometimes slow to index new releases, so check central just in case!
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
  id("com.gradle.enterprise") version "3.12.4"
}

gradleEnterprise {
  buildScan {
    termsOfServiceAgree = "yes"
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
  }
}

rootProject.name = "Catchup"

include(
  ":app",
  ":libraries:base-ui",
  ":libraries:appconfig",
  ":libraries:compose-extensions",
  ":libraries:di",
  ":libraries:di:android",
  ":libraries:gemoji",
  ":libraries:gemoji:db",
  ":libraries:gemoji:generator",
  ":libraries:flowbinding",
  ":libraries:kotlinutil",
  ":libraries:newark",
  ":libraries:retrofitconverters",
  ":libraries:tooling:spi-multibinds-validator",
  ":libraries:tooling:spi-visualizer",
  ":libraries:util",
  ":service-api",
  ":services:designernews",
  ":services:dribbble",
  ":services:github",
  ":services:hackernews",
//    ":services:imgur",
//  ":services:medium",
//    ":services:newsapi",
  ":services:producthunt",
  ":services:reddit",
  ":services:slashdot",
  ":services:unsplash",
  ":services:uplabs",
  ":platform",
)

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// https://github.com/gradle/gradle/blob/9a08b2368ca049a33783781c7810a7d2f4aaeab2/subprojects/docs/src/docs/userguide/running-builds/configuration_cache.adoc#stable-configuration-cache
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
