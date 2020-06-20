/*
 * Copyright (c) 2020 Zac Sweers
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
import com.android.build.gradle.internal.tasks.factory.dependsOn
import de.undercouch.gradle.tasks.download.Download

plugins {
  `java-library`
  id("de.undercouch.download")
}

val threeten = configurations.maybeCreate("threeten")
dependencies {
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.core)
  dependencies.add(threeten.name, "com.gabrielittner.threetenbp:compiler:0.9.0")

  testImplementation(deps.test.junit)
  testImplementation(deps.test.truth)
}

// TODO make this an actual gradle plugin
val tzdbVersion = providers.gradleProperty("tzdbVersion")
    .forUseAtConfigurationTime()
val tzDbOutput = tzdbVersion.flatMap {
  layout.buildDirectory.file("tzdb/$it/download/${it}.tar.gz")
}
    .map { it.asFile }
val downloadTzdb = tasks.register<Download>("downloadTzdb") {
  src(tzdbVersion.map { "https://data.iana.org/time-zones/releases/tzdata${it}.tar.gz" })
  dest(tzDbOutput)
}

val unzippedOutput = tzdbVersion.flatMap { layout.buildDirectory.dir("tzdb/$it/unpacked/$it") }
val unzipTzData = tasks.register<Copy>("unzipTzdb") {
  from(tzDbOutput.map { tarTree(resources.gzip(it)) })
  into(unzippedOutput)
}
unzipTzData.dependsOn(downloadTzdb)

abstract class GenerateTzDataTask : JavaExec() {
  @get:Input
  abstract val tzVersion: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputDirectory
  abstract val inputDir: Property<File>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:OutputDirectory
  abstract val tzOutputDir: DirectoryProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
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

val generateTzDat = tasks.register<GenerateTzDataTask>("generateTzData") {
  classpath(threeten)
  mainClass.set("com.gabrielittner.threetenbp.LazyZoneRulesCompiler")
  tzVersion.set(tzdbVersion)
  inputDir.set(unzipTzData.map { it.destinationDir })
  argumentProviders.add(CommandLineArgumentProvider(::computeArguments))

  // Note: we generate and check these in rather than count on gradle caching
  tzOutputDir.set(layout.projectDirectory.dir("src/main/resources"))
  codeOutputDir.set(layout.projectDirectory.dir("src/main/java"))
}
