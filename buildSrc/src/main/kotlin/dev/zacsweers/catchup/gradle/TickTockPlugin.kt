package dev.zacsweers.catchup.gradle

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

class TickTockPlugin : Plugin<Project> {

  companion object {
    const val INTERMEDIATES = "intermediates/ticktock"
  }

  override fun apply(project: Project) {
    project.setup()
  }

  private fun Project.setup() {
    // TODO allow customizing this?
    val threeten = configurations.maybeCreate("threeten")
    dependencies.add(threeten.name, "com.gabrielittner.threetenbp:compiler:0.9.0")

    val extension = extensions.create<TickTockExtension>("tickTock")

    val tzdbVersion = extension.tzVersion
    val tzDbOutput = tzdbVersion.flatMap {
      layout.buildDirectory.file("$INTERMEDIATES/$it/download/${it}.tar.gz")
    }
        .map { it.asFile }
    val downloadTzdb = tasks.register<Download>("downloadTzdb") {
      src(tzdbVersion.map { "https://data.iana.org/time-zones/releases/tzdata${it}.tar.gz" })
      dest(tzDbOutput)
    }

    val unzippedOutput = tzdbVersion.flatMap {
      layout.buildDirectory.dir("$INTERMEDIATES/$it/unpacked/$it")
    }
    val unzipTzData = tasks.register<Copy>("unzipTzdb") {
      from(tzDbOutput.map { tarTree(resources.gzip(it)) })
      into(unzippedOutput)
    }
    unzipTzData.dependsOn(downloadTzdb)

    tasks.register<GenerateZoneRuleFilesTask>("generateTzData") {
      classpath(threeten)
      mainClass.set("com.gabrielittner.threetenbp.LazyZoneRulesCompiler")
      tzVersion.set(tzdbVersion)
      inputDir.set(unzipTzData.map { it.destinationDir })
      argumentProviders.add(CommandLineArgumentProvider(::computeArguments))

      tzOutputDir.set(extension.tzOutputDir)
      codeOutputDir.set(extension.codeOutputDir)
    }
  }

  private fun <T : Task> TaskProvider<out T>.dependsOn(
      vararg tasks: TaskProvider<out Task>
  ): TaskProvider<out T> = apply {
    if (tasks.isNotEmpty()) {
      configure { dependsOn(*tasks) }
    }
  }
}

interface TickTockExtension {
  val tzVersion: Property<String>
  val tzOutputDir: DirectoryProperty
  val codeOutputDir: DirectoryProperty
}

@CacheableTask
abstract class GenerateZoneRuleFilesTask : JavaExec() {
  @get:Input
  abstract val tzVersion: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputDirectory
  abstract val inputDir: Property<File>

  @get:OutputDirectory
  abstract val tzOutputDir: DirectoryProperty

  @get:OutputDirectory
  abstract val codeOutputDir: DirectoryProperty

  fun computeArguments(): List<String> {
    return listOf(
        "--srcdir",
        inputDir.get().canonicalPath,
        "--codeoutdir",
        codeOutputDir.get().asFile.canonicalPath,
        "--tzdboutdir",
        tzOutputDir.get().asFile.canonicalPath,
        "--version",
        tzVersion.get()
    )
  }
}