/*
 * Copyright (C) 2020. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UnstableApiUsage")

package dev.zacsweers.catchup.gradle

import build
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import deps
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

private val PLATFORM_CONFIGURATIONS = setOf(
    "annotationProcessor",
    "api",
    "compile",
    "compileOnly",
    "implementation",
    "kapt",
    "runtimeOnly"
)

@Suppress("unused")
class CatchUpPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.subprojects {
      configureJvm()
    }
  }
}

private fun isPlatformConfigurationName(name: String): Boolean {
  // Kapt and compileOnly are special cases since they can be combined with others
  if (name.startsWith("kapt", ignoreCase = true) || name == "compileOnly") {
    return true
  }
  // Try trimming the flavor by just matching the suffix
  PLATFORM_CONFIGURATIONS.forEach { platformConfig ->
    if (name.endsWith(platformConfig, ignoreCase = true)) {
      return true
    }
  }
  return false
}

private fun Project.configureJvm() {
  configureAndroid()
  configureKotlin()
  configureJava()

  // Apply platforms
  configurations
      .matching { isPlatformConfigurationName(it.name) }
      .configureEach {
        dependencies {
          add(name, platform(deps.okhttp.bom))
        }
      }
}

private val baseExtensionConfig: BaseExtension.() -> Unit = {
  compileSdkVersion(deps.android.build.compileSdkVersion)
  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    vectorDrawables.useSupportLibrary = true
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    isCoreLibraryDesugaringEnabled = true
  }
  lintOptions {
    isCheckReleaseBuilds = false
    isAbortOnError = false
  }
  sourceSets {
    findByName("main")?.java?.srcDirs("src/main/kotlin")
    findByName("debug")?.java?.srcDirs("src/debug/kotlin")
    findByName("release")?.java?.srcDirs("src/release/kotlin")
    findByName("test")?.java?.srcDirs("src/test/kotlin")
  }
}

private fun Project.configureAndroid() {
  plugins.withType<AppPlugin> {
    // NOTE: BaseAppModuleExtension is internal. This will be replaced by a public
    // interface
    extensions.getByType<BaseAppModuleExtension>().apply {
      baseExtensionConfig()
      defaultConfig {
        targetSdkVersion(deps.android.build.targetSdkVersion)
      }
      if (deps.build.ci) {
        // This is what github actions has available
        // Necessary due to AGP bug https://issuetracker.google.com/issues/143630825
        ndkVersion = "21.0.6113669"
      }
      configureVersioning(project)
      lintOptions {
        lintConfig = rootProject.file("lint.xml")
        isAbortOnError = true
        check("InlinedApi")
        check("Interoperability")
        check("NewApi")
        fatal("NewApi")
        fatal("InlinedApi")
        enable("UnusedResources")
        isCheckReleaseBuilds = true
        textReport = deps.build.ci
        textOutput("stdout")
        isCheckDependencies = true

        // Pending fix in https://android-review.googlesource.com/c/platform/frameworks/support/+/1217923
        disable("UnsafeExperimentalUsageError", "UnsafeExperimentalUsageWarning")
      }
      buildTypes {
        getByName("debug") {
          setMatchingFallbacks("release")
          isDefault = true
        }
      }
    }

    dependencies.add("coreLibraryDesugaring", deps.build.coreLibraryDesugaring)
  }
  plugins.withType<LibraryPlugin> {
    extensions.getByType<LibraryExtension>().apply {
      baseExtensionConfig()

      variantFilter {
        ignore = buildType.name != "release"
      }
    }

    dependencies.add("coreLibraryDesugaring", deps.build.coreLibraryDesugaring)
  }
}

private fun Project.configureKotlin() {
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      languageVersion = "1.4"
      jvmTarget = "1.8"
      @Suppress("SuspiciousCollectionReassignment")
      freeCompilerArgs += build.standardFreeKotlinCompilerArgs
    }
  }
  plugins.withId("org.jetbrains.kotlin.kapt") {
    extensions.getByType<KaptExtension>().apply {
      correctErrorTypes = true
//      mapDiagnosticLocations = true
    }
  }
}

private fun Project.configureJava() {
  plugins.withType<JavaBasePlugin> {
    extensions.getByType<JavaPluginExtension>().apply {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }
  }
}

// Adapted from https://github.com/ducrohet/versionCode-4.0-sample
private fun BaseAppModuleExtension.configureVersioning(project: Project) {
  // use filter to apply onVariantProperties to a subset of the variants
  onVariantProperties.withBuildType("release") {
    val versionCodeTask = project.tasks.register<VersionCodeTask>(
        "computeVersionCodeFor${name.capitalize(Locale.US)}") {
      group = "versioning"
      outputFile.set(project.layout.buildDirectory.file("intermediates/versioning/versionCode.txt"))
    }
    val mappedVersionCodeTask = versionCodeTask.map { it.outputFile.get().asFile.readText().toInt() }
    val versionNameTask = project.tasks.register<VersionNameTask>(
        "computeVersionNameFor${name.capitalize(Locale.US)}") {
      group = "versioning"
      outputFile.set(project.layout.buildDirectory.file("intermediates/versioning/versionName.txt"))
    }
    val mappedVersionNameTask = versionNameTask.map { it.outputFile.get().asFile.readText() }

    // Have to iterate outputs because of APK splits.
    outputs.forEach { variantOutput ->
      variantOutput.versionCode.set(mappedVersionCodeTask)
      variantOutput.versionName.set(mappedVersionNameTask)
    }
  }
}
