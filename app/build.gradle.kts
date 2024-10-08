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
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okio.buffer
import okio.sink
import okio.source

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.licensee)
  alias(libs.plugins.bugsnag)
  alias(libs.plugins.baselineprofile)
  //  alias(libs.plugins.playPublisher)
}

val useDebugSigning: Boolean =
  providers.gradleProperty("useDebugSigning").orElse("false").map { it.toBoolean() }.get()

android {
  namespace = "dev.zacsweers.catchup.apk"
  defaultConfig {
    applicationId = "dev.zacsweers.catchup"

    // These are for debug only. Release versioning is set by CatchUpPlugin
    versionCode = 1
    versionName = "1.0"

    configure<BasePluginExtension> { archivesName.set("catchup") }

    manifestPlaceholders["BUGSNAG_API_KEY"] = "placeholder"
  }
  buildFeatures {
    buildConfig = true
    resValues = true
  }
  signingConfigs {
    if (!useDebugSigning && rootProject.file("signing/app-release.jks").exists()) {
      create("release") {
        keyAlias = "catchupkey"
        storeFile = rootProject.file("signing/app-release.jks")
        storePassword = providers.gradleProperty("catchup_signing_store_password").getOrElse("")
        keyPassword = providers.gradleProperty("catchup_signing_key_password").getOrElse("")
      }
    } else {
      create("release").initWith(getByName("debug"))
    }
  }
  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-dev"
    }
    getByName("release") {
      manifestPlaceholders["BUGSNAG_API_KEY"] =
        providers.gradleProperty("catchup_bugsnag_key").getOrElse("")
      signingConfig = signingConfigs.getByName(if (useDebugSigning) "debug" else "release")
      proguardFiles += file("proguard-rules.pro")
      isMinifyEnabled = true
      isShrinkResources = true
    }
  }
  splits {
    abi {
      isEnable = false // For baseline profile gen - https://issuetracker.google.com/285398001
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }
  experimentalProperties["android.experimental.art-profile-r8-rewriting"] = true
  experimentalProperties["android.experimental.r8.dex-startup-optimization"] = true
}

val automaticBaselineProfileGeneration = providers.environmentVariable("AUTOMATIC_BASELINE_GENERATION").getOrElse("false").toBoolean()

if (!automaticBaselineProfileGeneration) {
  tasks.named { it == "mergeProductionReleaseStartupProfile"}.configureEach {
    mustRunAfter(":app:copyProductionReleaseBaselineProfileIntoSrc")
  }
}

baselineProfile {
  // Don't build on every iteration of a full assemble.
  // Instead enable generation directly for the release build variant.
  automaticGenerationDuringBuild = automaticBaselineProfileGeneration

  // Don't save the profiles in source, generate adhoc
  saveInSrc = !automaticBaselineProfileGeneration

  from(projects.benchmark.dependencyProject)

  if (automaticBaselineProfileGeneration) {
    variants {
      maybeCreate("release").apply {
        // Ensure Baseline Profile is fresh for release builds.
        automaticGenerationDuringBuild = true
      }
    }
  }

  warnings {
    maxAgpVersion = false
  }
}

bugsnag {
  enabled.set(false) // Reenable whenever this matters
  overwrite.set(true)
}

// play {
//  track = "alpha"
//  serviceAccountEmail = properties["catchup_play_publisher_account"].toString()
//  serviceAccountCredentials = rootProject.file("signing/play-account.p12")
// }

abstract class CutChangelogTask : DefaultTask() {

  @get:Input abstract val versionName: Property<String>

  @TaskAction
  fun run() {
    val changelog = project.rootProject.file("CHANGELOG.md")

    val whatsNewPath = "${project.projectDir}/src/main/play/release-notes/en-US/default.txt"
    val newChangelog =
      getChangelog(changelog, "").let {
        if (it.length > 500) {
          logger.log(
            LogLevel.WARN,
            "Changelog length (${it.length}) exceeds 500 char max. Truncating...",
          )
          val warning = "\n(Truncated due to store restrictions. Full changelog in app!)"
          val warningLength = warning.length
          val remainingAmount = 500 - warningLength
          val builder = StringBuilder()
          for (line in it.lineSequence()) {
            if (builder.length + line.length + 1 < remainingAmount) {
              builder.appendLine(line)
            } else {
              break
            }
          }
          builder.append(warning).toString()
        } else it
      }
    if (newChangelog.isNotEmpty()) {
      project.file(whatsNewPath).writer().use { it.write(newChangelog) }
    }

    val currentContent = changelog.reader().readText()
    changelog.writer().use {
      it.apply {
        write("\n\n## ${versionName.get()} (${SimpleDateFormat("yyyy-MM-dd").format(Date())})\n")
        append(currentContent)
      }
    }
  }

  private fun getChangelog(changelog: File, defaultIfEmpty: String): String {
    val log = StringBuilder()
    changelog.reader().useLines { lines ->
      val iterator = lines.iterator()
      while (iterator.hasNext()) {
        val line = iterator.next()
        if (line.startsWith("#")) {
          break
        } else if (line.isNotEmpty()) {
          log.append(line).append("\n")
        }
      }
    }

    val finalLog = log.toString().trim()
    return finalLog.ifEmpty { defaultIfEmpty }
  }
}

val tagProvider =
  providers
    .exec {
      commandLine("git")
      args("describe", "--tags")
    }
    .standardOutput
    .asText
    .map { it.trimEnd() }

tasks.register("cutChangelog", CutChangelogTask::class.java) {
  versionName.set(tagProvider)
  group = "build"
  description = "Cuts the current changelog version and updates the play store changelog file"
}

fun getChangelog(): String {
  // Only return a changelog if we're publishing a release
  if (!hasProperty("includeChangelog")) {
    return ""
  }

  val changelog = rootProject.file("CHANGELOG.md")

  val log = StringBuilder()
  changelog.reader().useLines { lines ->
    val iterator = lines.iterator()
    var seenChanges = false
    var headerCount = 0
    while (iterator.hasNext()) {
      val line = iterator.next()
      if (line.startsWith("#")) {
        // We might see the first header from the last cut here. If so, skip it till the next
        if (seenChanges || headerCount > 0) {
          break
        } else {
          headerCount++
        }
      } else if (line.isNotEmpty()) {
        seenChanges = true
        log.append(line).append("\n")
      }
    }
  }
  return log.toString().trim()
}

abstract class UpdateVersion
@Inject
constructor(providers: ProviderFactory, private val execOps: ExecOperations) : DefaultTask() {

  @get:Option(
    option = "updateType",
    description = "Configures the version update type. Can be (major|minor|patch).",
  )
  @get:Input
  abstract val type: Property<String>

  @get:Input
  val latestTag =
    providers
      .exec {
        commandLine("git")
        args("describe", "--abbrev=0", "--tags")
      }
      .standardOutput
      .asText
      .map { it.trim() }

  @TaskAction
  fun run() {
    val latestTag =
      latestTag.get().takeUnless { it.isEmpty() }
        ?: throw IllegalStateException("No recent tag found!")
    var (major, minor, patch) = latestTag.split(".").map(String::toInt)
    when (type.get()) {
      "major" -> {
        major++
        minor = 0
        patch = 0
      }
      "minor" -> {
        minor++
        patch = 0
      }
      "patch" -> {
        patch++
      }
      else -> {
        throw IllegalArgumentException("Unrecognized updateType \"$type\"")
      }
    }
    val latestVersionString = "$major.$minor.$patch"
    println("Updating version to $latestVersionString")
    ByteArrayOutputStream().use { os ->
      project.rootProject.exec {
        commandLine(
          "git",
          "tag",
          "-a",
          latestVersionString,
          "-m",
          "\"Version $latestVersionString.\"",
        )
        standardOutput = os
        errorOutput = os
      }

      val newTagStream = ByteArrayOutputStream()
      newTagStream.use { innerOs ->
        execOps.exec {
          commandLine("git", "describe", "--abbrev=0", "--tags")
          standardOutput = innerOs
          errorOutput = innerOs
        }
      }
      val newTag = newTagStream.toString()
      if (newTag == latestTag) {
        throw AssertionError("Git tag didn't work! ${os.toString().trim()}")
      }
    }
  }
}

tasks.create("updateVersion", UpdateVersion::class.java) {
  group = "build"
  description =
    "Updates the current version. Supports CLI option --updateType={type} where type is (major|minor|patch)"
}

abstract class GenerateLicensesAsset : DefaultTask() {
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputDirectory
  abstract val buildDir: DirectoryProperty

  @get:OutputFile abstract val jsonFile: RegularFileProperty

  private val licenseeFile: File
    get() = File(buildDir.asFile.get(), "reports/licensee/androidRelease/artifacts.json")

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalStdlibApi::class)
  @TaskAction
  fun generate() {
    val mapAdapter = Moshi.Builder().build().adapter<List<Map<String, Any>>>()
    val githubDetailsMap =
      JsonReader.of(licenseeFile.source().buffer()).use { mapAdapter.fromJson(it)!! }
    val githubDetails =
      githubDetailsMap.mapNotNull { entry ->
        entry["scm"]?.let {
          ((it as? Map<String, Any>)?.get("url") as? String?)?.let { url -> parseScm(url) }
        }
      }

    JsonWriter.of(jsonFile.get().asFile.sink().buffer()).use { writer ->
      writer.beginArray()
      githubDetails
        .sortedBy { it.toString().lowercase(Locale.US) }
        .distinctBy { it.toString().lowercase(Locale.US) }
        .forEach { (owner, name) ->
          writer.beginObject()
          writer.name("owner").value(owner).name("name").value(name)
          writer.endObject()
        }
      writer.endArray()
    }
  }

  private fun parseScm(url: String): Pair<String, String>? {
    if ("github.com" !in url) return null
    val parts =
      url
        .substringAfter("github.com")
        .removePrefix("/")
        .removePrefix(":")
        .removeSuffix(".git")
        .removeSuffix("/issues")
        .substringAfter(".com/")
        .trim()
        .removeSuffix("/")
        .split("/")
    val owner = parts.getOrNull(0) ?: return null
    val name = parts.getOrNull(1) ?: return null
    return owner to name
  }
}

val generateLicenseTask =
  tasks.register<GenerateLicensesAsset>("generateLicensesAsset") {
    buildDir.set(project.layout.buildDirectory)
    jsonFile.set(project.layout.projectDirectory.file("src/main/assets/generated_licenses.json"))
  }

generateLicenseTask.dependsOn("licenseeAndroidRelease")

tasks.named { it.startsWith("licensee") && !it.endsWith("AndroidRelease") }
  .configureEach { enabled = false }

licensee {
  allow("Apache-2.0")
  allow("MIT")
  allowUrl("http://opensource.org/licenses/BSD-2-Clause")
  allowUrl("https://developer.android.com/studio/terms.html")
  allowUrl("https://jsoup.org/license")
  // MIT
  allowUrl("https://github.com/alorma/Compose-Settings/blob/main/LICENSE")
  // MIT
  allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE")
  // MIT
  allowUrl("https://github.com/facebook/flipper/blob/main/LICENSE")
  allowUrl("https://github.com/facebook/soloader/blob/main/LICENSE")
  allowUrl("https://github.com/facebookincubator/fbjni/blob/main/LICENSE")
  allowUrl("https://github.com/TooTallNate/Java-WebSocket/blob/master/LICENSE")
  allowUrl("https://www.openssl.org/source/license-openssl-ssleay.txt")
  // MIT
  allowUrl("https://opensource.org/licenses/MIT")
}

androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    variant.packaging.resources.excludes.addAll(
      "**/*.dot",
      "**/*.kotlin_metadata",
      "**/*.properties",
      "*.properties",
      "kotlin/**",
      "LICENSE.txt",
      "LICENSE_OFL",
      "LICENSE_UNICODE",
      "META-INF/*.kotlin_module",
      "META-INF/*.version",
      "META-INF/androidx.*",
      "META-INF/CHANGES",
      "META-INF/LICENSE",
      "META-INF/LICENSE.txt",
      "META-INF/NOTICE",
      "META-INF/NOTICE.txt",
      "META-INF/README.md",
    )
  }
}

dependencies { implementation(projects.appScaffold) }
