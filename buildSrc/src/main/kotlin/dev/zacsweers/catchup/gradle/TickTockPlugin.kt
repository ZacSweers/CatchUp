package dev.zacsweers.catchup.gradle

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
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
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import java.io.File
import javax.inject.Inject

class TickTockPlugin : Plugin<Project> {

  companion object {
    const val INTERMEDIATES = "intermediates/ticktock"
  }

  override fun apply(project: Project) {
    project.setup()
  }

  private fun Project.setup() {
    // TODO allow customizing this?
    val lazyThreeTen = configurations.maybeCreate("lazyThreeTen")
    dependencies.add(lazyThreeTen.name, "com.gabrielittner.threetenbp:compiler:0.9.0")

    val threetenbp = configurations.maybeCreate("threetenbp")
    dependencies.add(threetenbp.name, "org.threeten:threetenbp:1.4.4")

    val extension = extensions.create<TickTockExtension>("tickTock")

    val tzdbVersion = extension.tzVersion
    val tzDbOutput = tzdbVersion.flatMap {
      layout.buildDirectory.file("$INTERMEDIATES/$it/download/${it}.tar.gz")
    }
        .map { it.asFile }
    val downloadTzdb = tasks.register<Download>("downloadTzData") {
      src(tzdbVersion.map { "https://data.iana.org/time-zones/releases/tzdata${it}.tar.gz" })
      dest(tzDbOutput)
    }

    val unzippedOutput = tzdbVersion.flatMap {
      layout.buildDirectory.dir("$INTERMEDIATES/$it/unpacked/$it")
    }
    val unzipTzData = tasks.register<Copy>("unzipTzdata") {
      from(tzDbOutput.map { tarTree(resources.gzip(it)) })
      into(unzippedOutput)
    }
    unzipTzData.dependsOn(downloadTzdb)

    // Set up lazy rules
    tasks.register<GenerateZoneRuleFilesTask>("generateLazyZoneRules") {
      classpath(lazyThreeTen)
      mainClass.set("com.gabrielittner.threetenbp.LazyZoneRulesCompiler")
      tzVersion.set(tzdbVersion)
      inputDir.set(unzipTzData.map { it.destinationDir })
      argumentProviders.add(CommandLineArgumentProvider(::computeArguments))

      tzOutputDir.set(extension.tzOutputDir)
      codeOutputDir.set(extension.codeOutputDir)
    }

    val tzdatOutputDir = tzdbVersion.flatMap { layout.buildDirectory.dir("$INTERMEDIATES/$it/dat") }
    val generateTzDat = tasks.register<GenerateTzDatTask>("generateTzDat") {
      classpath(threetenbp)
      mainClass.set("org.threeten.bp.zone.TzdbZoneRulesCompiler")
      tzVersion.set(tzdbVersion)
      inputDir.set(unzipTzData.map { it.destinationDir.parentFile })
      outputDir.set(tzdatOutputDir)
      argumentProviders.add(CommandLineArgumentProvider(::computeArguments))
    }

    tasks.register<Copy>("copyTzDatToResources") {
      from(generateTzDat.map { it.outputDir })
      into(extension.tzOutputDir.map { it.dir("j\$/time/zone") })
      // The CLI outputs TZDB.dat but we want lowercase
      rename {
        it.replace("TZDB.dat", "tzdb.dat")
      }
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

abstract class TickTockExtension @Inject constructor(
    layout: ProjectLayout,
    objects: ObjectFactory
) {
  val tzVersion: Property<String> = objects.property<String>()
      .convention("2020a")

  val resourcesDir: DirectoryProperty = objects.directoryProperty()
      .convention(layout.projectDirectory.dir("src/main/resources"))

  val tzOutputDir: DirectoryProperty = objects.directoryProperty()
      .convention(resourcesDir)

  abstract val codeOutputDir: DirectoryProperty
}

/** A zone rules generation task for granular zone rules. */
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

/** A zone rules generation task for `tzdb.dat`. */
@CacheableTask
abstract class GenerateTzDatTask : JavaExec() {
  @get:Input
  abstract val tzVersion: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputDirectory
  abstract val inputDir: Property<File>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  fun computeArguments(): List<String> {
    return listOf(
        "-srcdir",
        inputDir.get().canonicalPath,
        "-dstdir",
        outputDir.get().asFile.canonicalPath,
        "-version",
        tzVersion.get(),
        "-unpacked"
    )
  }
}