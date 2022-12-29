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
import java.util.Locale.US
import okio.buffer
import okio.sink
import okio.source

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  kotlin("plugin.parcelize")
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.apollo)
  alias(libs.plugins.licensee)
  alias(libs.plugins.moshix)
  alias(libs.plugins.anvil)
  alias(libs.plugins.ksp)
  //  alias(libs.plugins.bugsnag)
  //  alias(libs.plugins.playPublisher)
}

slack {
  features { dagger(enableComponents = true) { alwaysEnableAnvilComponentMerging() } }
  android { features { compose() } }
}

val useDebugSigning: Boolean =
  providers.gradleProperty("useDebugSigning").orElse("false").map { it.toBoolean() }.get()

android {
  defaultConfig {
    applicationId = "io.sweers.catchup"

    // These are for debug only. Release versioning is set by CatchUpPlugin
    versionCode = 1
    versionName = "1.0"

    configure<BasePluginExtension> { archivesName.set("catchup") }

    buildConfigField(
      "String",
      "GITHUB_DEVELOPER_TOKEN",
      "\"${properties["catchup_github_developer_token"]}\""
    )
    resValue("string", "changelog_text", "haha")
    manifestPlaceholders["BUGSNAG_API_KEY"] = "placeholder"
  }
  buildFeatures {
    buildConfig = true
    compose = true
    resValues = true
    viewBinding = true
  }
  signingConfigs {
    if (!useDebugSigning && rootProject.file("signing/app-release.jks").exists()) {
      create("release") {
        keyAlias = "catchupkey"
        storeFile = rootProject.file("signing/app-release.jks")
        storePassword = properties["catchup_signing_store_password"].toString()
        keyPassword = properties["catchup_signing_key_password"].toString()
      }
    } else {
      create("release") // .initWith(getByName("debug"))
    }
  }
  packagingOptions.resources.excludes +=
    listOf(
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
      "META-INF/com.uber.crumb/**",
      "META-INF/LICENSE",
      "META-INF/LICENSE.txt",
      "META-INF/NOTICE",
      "META-INF/NOTICE.txt",
      "META-INF/README.md",
      "META-INF/rxjava.properties",
      "META-INF/services/javax.annotation.processing.Processor",
    )
  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-dev"
      buildConfigField(
        "String",
        "IMGUR_CLIENT_ACCESS_TOKEN",
        "\"${project.properties["catchup_imgur_access_token"]}\""
      )
    }
    getByName("release") {
      buildConfigField("String", "BUGSNAG_KEY", "\"${properties["catchup_bugsnag_key"]}\"")
      manifestPlaceholders["BUGSNAG_API_KEY"] = properties["catchup_bugsnag_key"].toString()
      signingConfig = signingConfigs.getByName(if (useDebugSigning) "debug" else "release")
      proguardFiles += file("proguard-rules.pro")
      isMinifyEnabled = true
      isShrinkResources = true
    }
  }
  splits {
    density {
      isEnable = true
      reset()
      include("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    }
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }
  lint {
    disable += "Typos" // https://twitter.com/ZacSweers/status/1495491162920136706
    disable += "MissingTranslation"
    disable += "ExtraTranslation" // wrong?
    disable += "VectorPath" // Always complains about long paths as if I could do something about it
    checkDependencies = true
  }
  namespace = "io.sweers.catchup"
}

// bugsnag {
//   // Prevent bugsnag from wiring build UUIDs into debug builds
//   variantFilter {
//     setEnabled("debug" !in name.toLowerCase(Locale.US))
//   }
// }

kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }

// play {
//  track = "alpha"
//  serviceAccountEmail = properties["catchup_play_publisher_account"].toString()
//  serviceAccountCredentials = rootProject.file("signing/play-account.p12")
// }

apollo {
  service("github") {
    customScalarsMapping.set(
      mapOf("DateTime" to "kotlinx.datetime.Instant", "URI" to "okhttp3.HttpUrl")
    )
    packageName.set("io.sweers.catchup.data.github")
    schemaFile.set(file("src/main/graphql/io/sweers/catchup/data/github/schema.json"))
  }
}

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
            "Changelog length (${it.length}) exceeds 500 char max. Truncating..."
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
constructor(
  providers: ProviderFactory,
  private val execOps: ExecOperations,
) : DefaultTask() {

  @get:Option(
    option = "updateType",
    description = "Configures the version update type. Can be (major|minor|patch)."
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
          "\"Version $latestVersionString.\""
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
    get() = File(buildDir.asFile.get(), "reports/licensee/release/artifacts.json")

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
        .sortedBy { it.toString().toLowerCase(US) }
        .distinctBy { it.toString().toLowerCase(US) }
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
    val (owner, name) =
      url
        .substringAfter("github.com")
        .removePrefix("/")
        .removePrefix(":")
        .removeSuffix(".git")
        .removeSuffix("/issues")
        .substringAfter(".com/")
        .split("/")
    return owner to name
  }
}

val generateLicenseTask =
  tasks.register<GenerateLicensesAsset>("generateLicensesAsset") {
    buildDir.set(project.layout.buildDirectory)
    jsonFile.set(project.layout.projectDirectory.file("src/main/assets/generated_licenses.json"))
  }

generateLicenseTask.dependsOn("licenseeRelease")

tasks.matching { it.name.startsWith("licensee") }.configureEach {
  notCompatibleWithConfigurationCache("https://github.com/cashapp/licensee/issues/72")
}

tasks.matching { it.name == "licenseeDebug" }.configureEach {
  enabled = false
}

licensee {
  allow("Apache-2.0")
  allow("MIT")
  allow("MIT-0")
  allowUrl("https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE")
  allowUrl("http://opensource.org/licenses/BSD-2-Clause")
  allowUrl("https://developer.android.com/studio/terms.html")
  allowUrl("https://jsoup.org/license")
}

dependencies {
  kapt(project(":libraries:tooling:spi-visualizer"))
  kapt(project(":libraries:tooling:spi-multibinds-validator"))
  kapt(libs.androidx.room.apt)

  implementation(libs.markwon.core)
  implementation(libs.markwon.extStrikethrough)
  implementation(libs.markwon.extTables)
  implementation(libs.markwon.extTasklist)
  implementation(libs.markwon.html)
  implementation(libs.markwon.image)
  implementation(libs.markwon.imageCoil)
  implementation(libs.markwon.linkify)
  //  implementation(libs.markwon.syntaxHighlight) // https://github.com/noties/Markwon/issues/148
  implementation(project(":service-api"))
  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:appconfig"))
  implementation(project(":libraries:gemoji"))
  implementation(project(":libraries:kotlinutil"))
  implementation(project(":libraries:smmry"))
  implementation(project(":libraries:util"))
  implementation(project(":libraries:flowbinding"))
  implementation(libs.misc.ticktock)

  // Support libs
  implementation(libs.androidx.annotations)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.core)
  implementation(libs.androidx.constraintLayout)
  implementation(libs.androidx.compose.constraintLayout)
  implementation(libs.androidx.customTabs)
  implementation(libs.androidx.design)
  implementation(libs.androidx.emoji)
  implementation(libs.androidx.emojiAppcompat)
  implementation(libs.androidx.fragment)
  implementation(libs.androidx.fragmentKtx)
  debugImplementation(libs.androidx.drawerLayout)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.preferenceKtx)
  implementation(libs.androidx.viewPager2)
  implementation(libs.androidx.swipeRefresh)

  // Arch components
  implementation(libs.androidx.lifecycle.extensions)
  kapt(libs.androidx.lifecycle.apt)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.rxJava3)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.paging)
  kapt(libs.androidx.room.apt)

  // Compose
  implementation(project(":libraries:compose-extensions"))
  implementation(libs.androidx.compose.uiTooling)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material)

  // Kotlin
  implementation(libs.androidx.coreKtx)
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.coroutinesAndroid)
  implementation(libs.kotlin.coroutinesRx)
  implementation(libs.androidx.lifecycle.ktx)

  // Moshi
  implementation(libs.moshi.core)
  implementation(libs.moshi.shimo)

  // Firebase
  implementation(libs.firebase.core)
  implementation(libs.firebase.database)

  // Square/JW
  implementation(libs.okhttp.core)
  implementation(libs.misc.okio)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.moshi)
  implementation(libs.retrofit.rxJava3)
  implementation(libs.rx.android)
  implementation(libs.rx.java)
  implementation(libs.misc.tapTargetView)
  implementation(libs.misc.timber)
  implementation(libs.misc.debug.processPhoenix)
  debugImplementation(libs.misc.debug.madge)
  debugImplementation(libs.misc.debug.scalpel)
  debugImplementation(libs.misc.debug.telescope)
  debugImplementation(libs.okhttp.debug.loggingInterceptor)
  debugImplementation(libs.retrofit.debug.mock)

  releaseImplementation(libs.misc.bugsnag)

  // Coil
  implementation(libs.coil.base)
  implementation(libs.coil.default)
  implementation(libs.coil.gif)

  // Misc
  implementation(libs.autodispose.core)
  implementation(libs.autodispose.android)
  implementation(libs.autodispose.lifecycle)
  implementation(libs.misc.byteunits)
  implementation(libs.misc.flick)
  implementation(libs.misc.gestureViews)
  implementation(libs.misc.inboxRecyclerView)
  implementation(libs.misc.lottie)
  implementation(libs.misc.recyclerViewAnimators)
  implementation(libs.rx.relay)
  implementation(libs.rx.dogTag)
  implementation(libs.rx.dogTagAutoDispose)
  implementation(libs.misc.moshiLazyAdapters)
  implementation(libs.autodispose.androidxLifecycle)
  implementation(libs.misc.kotpref)
  implementation(libs.misc.kotprefEnum)
  implementation(libs.kotlin.datetime)

  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.androidx.paging.compose)
  implementation(libs.circuit.core)
  ksp(libs.circuit.codegen)
  implementation(libs.circuit.codegenAnnotations)
  implementation(libs.circuit.overlay)
  implementation(libs.circuit.retained)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.coil.compose)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.activity.compose)

  // Apollo
  implementation(libs.apollo.httpcache)
  implementation(libs.apollo.normalizedCache)
  implementation(libs.apollo.runtime)

  implementation(project(":libraries:gemoji"))
  implementation(project(":services:designernews"))
  implementation(project(":services:dribbble"))
  implementation(project(":services:github"))
  implementation(project(":services:hackernews"))
  //  implementation(project(":services:medium"))
  implementation(project(":services:producthunt"))
  implementation(project(":services:reddit"))
  implementation(project(":services:slashdot"))
  implementation(project(":services:unsplash"))
  implementation(project(":services:uplabs"))
  //  implementation(project(":services:imgur"))
  //  implementation(project(":services:newsapi"))

  // Flipper
  debugImplementation(libs.misc.debug.flipper)
  debugImplementation(libs.misc.debug.flipperNetwork)
  debugImplementation(libs.misc.debug.soLoader)

  // To force a newer version that doesn't conflict ListenableFuture
  debugImplementation(libs.misc.debug.guava)

  // Hyperion
  //  releaseImplementation(libs.hyperion.core.release)
  //  debugImplementation(libs.hyperion.core.debug)
  //  debugImplementation(libs.hyperion.plugins.appInfo)
  //  debugImplementation(libs.hyperion.plugins.attr)
  //  debugImplementation(libs.hyperion.plugins.chuck)
  //  debugImplementation(libs.hyperion.plugins.crash)
  //  debugImplementation(libs.hyperion.plugins.disk)
  //  debugImplementation(libs.hyperion.plugins.geigerCounter)
  //  debugImplementation(libs.hyperion.plugins.measurement)
  //  debugImplementation(libs.hyperion.plugins.phoenix)
  //  debugImplementation(libs.hyperion.plugins.recorder)
  //  debugImplementation(libs.hyperion.plugins.sharedPreferences)
  //  debugImplementation(libs.hyperion.plugins.timber)

  implementation(libs.misc.jsr305)
  implementation(projects.libraries.di)
  implementation(projects.libraries.di.android)

  // Test
  testImplementation(libs.rx.relay)
  androidTestImplementation(libs.rx.java)
  androidTestImplementation(libs.misc.jsr305)
  testImplementation(libs.misc.jsr305)
  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)

  // LeakCanary
  debugImplementation(libs.misc.leakCanary)
  releaseImplementation(libs.misc.leakCanaryObjectWatcherAndroid)

  // Chuck
  //  debugImplementation(libs.chuck.debug)
  //  releaseImplementation(libs.chuck.release)
}
