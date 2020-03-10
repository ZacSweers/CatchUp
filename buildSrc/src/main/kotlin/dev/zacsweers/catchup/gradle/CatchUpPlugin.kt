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