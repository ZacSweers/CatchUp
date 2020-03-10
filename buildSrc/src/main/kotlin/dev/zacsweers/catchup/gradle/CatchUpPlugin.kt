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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.util.Locale

@Suppress("unused")
class CatchUpPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureVersioning()
  }
}

// Adapted from https://github.com/ducrohet/versionCode-4.0-sample
private fun Project.configureVersioning() {
  plugins.withType<AppPlugin> {
    // NOTE: BaseAppModuleExtension is internal. This will be replaced by a public
    // interface
    val extension = project.extensions.getByName("android") as BaseAppModuleExtension
    extension.configure(project)
  }
}

private fun BaseAppModuleExtension.configure(project: Project) {
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
