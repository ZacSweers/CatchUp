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

/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import de.triplet.gradle.play.PlayAccountConfig
import org.gradle.api.internal.plugins.DefaultConvention
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  id("com.android.application")
  id("io.sweers.psync")
  kotlin("android")
  kotlin("kapt")
  id("com.apollographql.android")
  id("net.ltgt.errorprone")
  id("com.bugsnag.android.gradle")
  id("com.github.triplet.play")
}

apply {
  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
  if (hasProperty("enableFirebasePerf")) {
    plugin("com.google.firebase.firebase-perf")
  }
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)
  buildToolsVersion(deps.android.build.buildToolsVersion)

  defaultConfig {
    applicationId = "io.sweers.catchup"
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
    versionCode = deps.build.gitCommitCount(project)
    versionName = deps.build.gitTag(project)
    multiDexEnabled = false

    the<BasePluginConvention>().archivesBaseName = "catchup"
    vectorDrawables.useSupportLibrary = true

    buildConfigField("String", "GIT_SHA", "\"${deps.build.gitSha(project)}\"")
    buildConfigField("long", "GIT_TIMESTAMP", deps.build.gitTimestamp(project).toString())
    buildConfigField("String", "GITHUB_DEVELOPER_TOKEN",
        "\"${properties["catchup_github_developer_token"]}\"")
    buildConfigField("String", "SMMRY_API_KEY",
        "\"${properties["catchup_smmry_api_key"]}\"")
    resValue("string", "changelog_text", "\"${getChangelog()}\"")
  }
  compileOptions {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
  }
  signingConfigs {
    create("release") {
      keyAlias = "catchupkey"
      storeFile = rootProject.file("signing/app-release.jks")
      storePassword = properties["catchup_signing_store_password"].toString()
      keyPassword = properties["catchup_signing_key_password"].toString()
      isV2SigningEnabled = true
    }
  }
  packagingOptions {
    exclude("META-INF/LICENSE")
    exclude("META-INF/NOTICE")
    exclude("LICENSE.txt")
    exclude("META-INF/rxjava.properties")
    exclude("META-INF/NOTICE.txt")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/services/javax.annotation.processing.Processor")
  }
  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-dev"
      ext["enableBugsnag"] = false
      buildConfigField("String", "IMGUR_CLIENT_ACCESS_TOKEN",
          "\"${project.properties["catchup_imgur_access_token"].toString()}\"")
    }
    getByName("release") {
      buildConfigField("String", "BUGSNAG_KEY",
          "\"${properties["catchup_bugsnag_key"].toString()}\"")
      signingConfig = signingConfigs.getByName("release")
      postprocessing.apply {
        proguardFiles("proguard-rules.pro")
        isOptimizeCode = true
        isObfuscate = true
      }
    }
  }
  dexOptions {
    javaMaxHeapSize = "2g"
  }
  lintOptions {
    setLintConfig(file("lint.xml"))
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
    htmlReport = !deps.build.ci
    xmlReport = !deps.build.ci
  }
  // Should be fixed now, disable if need be
  // https://github.com/bugsnag/bugsnag-android-gradle-plugin/issues/59
//  splits {
//    density {
//      isEnable = true
//      reset()
//      include("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
//    }
//    abi {
//      isEnable = true
//      reset()
//      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
//      isUniversalApk = true
//    }
//  }
  afterEvaluate {
    val firebaseVariants = setOf("release", "debug")
    applicationVariants.forEach { variant ->
      // Configure firebase
      fun firebaseProperty(property: String, resolveName: Boolean = true) {
        val buildTypeName = variant.buildType.name
        if (buildTypeName in firebaseVariants) {
          val name = if (resolveName && buildTypeName == "debug") {
            "$property.debug"
          } else property
          val value = project.properties[name].toString()
          variant.resValue("string", property.removePrefix("catchup."), value)
        } else {
          return
        }
      }
      firebaseProperty("catchup.google_api_key")
      firebaseProperty("catchup.google_app_id")
      firebaseProperty("catchup.firebase_database_url")
      firebaseProperty("catchup.ga_trackingId")
      firebaseProperty("catchup.gcm_defaultSenderId")
      firebaseProperty("catchup.google_storage_bucket")
      firebaseProperty("catchup.default_web_client_id")
      firebaseProperty("catchup.google_crash_reporting_api_key")
      firebaseProperty("catchup.project_id", false)
    }
  }
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  mapDiagnosticLocations = true
  arguments {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("moshi.generated", "javax.annotation.Generated")
  }
}

play {
  setTrack("alpha")
  uploadImages = true
  serviceAccountEmail = properties["catchup_play_publisher_account"].toString()
  pk12File = rootProject.file("signing/play-account.p12")
}

bugsnag {
  apiKey = properties["catchup_bugsnag_key"].toString()
  autoProguardConfig = false
  ndk = true
}

psync {
  includesPattern = "**/xml/prefs_*.xml"
  generateRx = true
  packageName = "io.sweers.catchup"
}

if (gradle.startParameter.isOffline) {
  afterEvaluate {
    // Because this stalls in offline mode
    tasks.findByName("installApolloCodegen")?.deleteAllActions()
  }
}

apollo {
  customTypeMapping["DateTime"] = "org.threeten.bp.Instant"
  customTypeMapping["URI"] = "okhttp3.HttpUrl"
}

tasks.withType<JavaCompile> {
  options.compilerArgs = listOf("-Xep:MissingOverride:OFF")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
  }
}

open class CutChangelogTask : DefaultTask() {

  @Input
  lateinit var versionName: String

  @TaskAction
  fun run() {
    val changelog = project.rootProject.file("CHANGELOG.md")

    val whatsNewPath = "${project.projectDir}/src/main/play/en-US/whatsnew"
    val newChangelog = getChangelog(changelog, "")
    if (newChangelog.length > 500) {
      throw IllegalStateException("Changelog length exceeds 500ch max. Is ${newChangelog.length}")
    }
    if (!newChangelog.isEmpty()) {
      project.file(whatsNewPath).writer().use {
        it.write(newChangelog)
      }
    }

    val currentContent = changelog.reader().readText()
    changelog.writer().use {
      it.apply {
        write("\n\n## $versionName (${SimpleDateFormat("yyyy-MM-dd").format(Date())})\n")
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
        } else if (!line.isEmpty()) {
          log.append(line).append("\n")
        }
      }
    }

    val finalLog = log.toString().trim()
    return if (finalLog.isEmpty()) defaultIfEmpty else finalLog
  }
}

tasks {
  "cutChangelog"(CutChangelogTask::class) {
    versionName = deps.build.gitTag(project)
    group = "build"
    description = "Cuts the current changelog version and updates the play store changelog file"
  }
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
      } else if (!line.isEmpty()) {
        seenChanges = true
        log.append(line).append("\n")
      }
    }
  }
  return log.toString().trim()
}

open class UpdateVersion : DefaultTask() {
  @Input
  lateinit var type: String

  @TaskAction
  fun run() {
    val workingDir = project.rootProject.projectDir
    val latestTag = "git describe --abbrev=0 --tags".execute(workingDir, "dev")
    if (latestTag == "dev") {
      throw IllegalStateException("No recent tag found!")
    }
    var (major, minor, patch) = latestTag.split(".").map(String::toInt)
    when (type) {
      "M" -> {
        major++
        minor = 0
        patch = 0
      }
      "m" -> {
        minor++
        patch = 0
      }
      "p" -> {
        patch++
      }
      else -> {
        throw IllegalArgumentException("Unrecognized version type \"$type\"")
      }
    }
    val latestVersionString = "$major.$minor.$patch"
    println("Updating version to $latestVersionString")
    ByteArrayOutputStream().use { os ->
      project.rootProject.exec {
        commandLine("git",
            "tag",
            "-a", latestVersionString,
            "-m", "\"Version $latestVersionString.\"")
        standardOutput = os
        errorOutput = os
      }
      val newTag = "git describe --abbrev=0 --tags".execute(workingDir, "dev")
      if (newTag == latestTag) {
        throw AssertionError("Git tag didn't work! ${os.toString().trim()}")
      }
    }
  }
}

tasks {
  "updateVersion"(UpdateVersion::class) {
    type = properties["version"].toString()
    group = "build"
    description = "Updates the current version. Supports CLI property flag -Pversion={type} where type is (Mmp)"
  }
}

dependencies {
  kapt(project(":tooling:spi-visualizer"))
  compileOnly(project(":tooling:spi-visualizer"))

  implementation(project(":third_party:bypass"))
  implementation(project(":service-api"))
  implementation(project(":service-registry:service-registry"))
  implementation(project(":gemoji"))
  implementation(project(":util"))

  // Support libs
  implementation(deps.android.support.annotations)
  implementation(deps.android.support.appCompat)
  implementation(deps.android.support.compat)
  implementation(deps.android.support.constraintLayout)
  implementation(deps.android.support.customTabs)
  implementation(deps.android.support.design)
  debugImplementation(deps.android.support.drawerLayout)
  implementation(deps.android.support.palette)
  implementation(deps.android.support.viewPager)
  implementation(deps.android.support.swipeRefresh)
  implementation(deps.android.support.legacyAnnotations)

  // Arch components
  implementation(deps.android.arch.lifecycle.extensions)
  kapt(deps.android.arch.lifecycle.apt)
  implementation(deps.android.arch.room.runtime)
  implementation(deps.android.arch.room.rxJava2)
  kapt(deps.android.arch.room.apt)

  // Kotlin
  implementation(deps.android.ktx)
  implementation(deps.kotlin.stdlib.jdk7)
  implementation(deps.kotlin.stdlib.jdk7)

  // Moshi
  kapt(deps.moshi.compiler)
  implementation(deps.moshi.core)

  // Firebase
  implementation(deps.android.firebase.core)
  implementation(deps.android.firebase.config)
  implementation(deps.android.firebase.database)
  implementation(deps.android.firebase.perf)

  // Square/JW
  implementation(deps.butterKnife.runtime)
  kapt(deps.butterKnife.apt)
  implementation(deps.okhttp.core)
  implementation(deps.misc.okio)
  implementation(deps.retrofit.core)
  implementation(deps.retrofit.moshi)
  implementation(deps.retrofit.rxJava2)
  implementation(deps.rx.android)
  implementation(deps.rx.java)
  implementation(deps.rx.binding.core)
  implementation(deps.rx.binding.v4)
  implementation(deps.rx.binding.design)
  implementation(deps.misc.lazythreeten)
  implementation(deps.misc.tapTargetView)
  implementation(deps.misc.timber)
  debugImplementation(deps.misc.debug.madge)
  debugImplementation(deps.misc.debug.scalpel)
  debugImplementation(deps.misc.debug.processPhoenix)
  debugImplementation(deps.misc.debug.telescope)
  debugImplementation(deps.okhttp.debug.loggingInterceptor)
  debugImplementation(deps.retrofit.debug.mock)

  releaseImplementation(deps.misc.bugsnag)

  // Glide
  kapt(deps.glide.apt.compiler)
  implementation(deps.glide.annotations)
  implementation(deps.glide.core)
  implementation(deps.glide.okhttp)
  implementation(deps.glide.recyclerView)

  // Misc
  implementation(deps.autoDispose.core)
  implementation(deps.autoDispose.android)
  implementation(deps.autoDispose.kotlin)
  errorprone(deps.errorProne.build.core)
  compileOnly(deps.errorProne.compileOnly.annotations)
  implementation(deps.barber.api)
  kapt(deps.barber.apt)
  implementation(deps.misc.lottie)
  implementation(deps.misc.recyclerViewAnimators)
  implementation(deps.rx.preferences)
  implementation(deps.rx.relay)
  implementation(deps.misc.moshiLazyAdapters)

  // Apollo
  implementation(deps.apollo.androidSupport)
  implementation(deps.apollo.httpcache)
  implementation(deps.apollo.runtime)
  implementation(deps.apollo.rx2Support)

  // Stetho
  debugImplementation(deps.stetho.debug.core)
  debugImplementation(deps.stetho.debug.okhttp)
  debugImplementation(deps.stetho.debug.timber)

  // Hyperion
  releaseImplementation(deps.hyperion.core.release)
  debugImplementation(deps.hyperion.core.debug)
  debugImplementation(deps.hyperion.plugins.appInfo)
  debugImplementation(deps.hyperion.plugins.attr)
  debugImplementation(deps.hyperion.plugins.chuck)
  debugImplementation(deps.hyperion.plugins.crash)
  debugImplementation(deps.hyperion.plugins.disk)
//  debugImplementation(deps.hyperion.plugins.geigerCounter)
  debugImplementation(deps.hyperion.plugins.measurement)
  debugImplementation(deps.hyperion.plugins.phoenix)
  debugImplementation(deps.hyperion.plugins.recorder)
  debugImplementation(deps.hyperion.plugins.sharedPreferences)

  // Dagger
  kapt(deps.dagger.apt.compiler)
  kapt(deps.dagger.android.apt.processor)
  compileOnly(deps.misc.javaxInject)
  implementation(deps.dagger.runtime)
  implementation(deps.dagger.android.runtime)

  // Inspector exposed for dagger
  implementation(deps.inspector.core)

  // Conductor
  implementation(deps.conductor.core)
  implementation(deps.conductor.autoDispose)
  implementation(deps.conductor.support)

  implementation(deps.misc.jsr305)

  // Test
  testImplementation(deps.rx.relay)
  androidTestCompileOnly(deps.errorProne.compileOnly.annotations)
  androidTestImplementation(deps.rx.java)
  androidTestImplementation(deps.misc.jsr305)
  testImplementation(deps.misc.jsr305)
  testCompileOnly(deps.errorProne.compileOnly.annotations)
  testImplementation(deps.test.junit)
  testImplementation(deps.test.truth)

  // LeakCanary
  debugImplementation(deps.leakCanary.debug)
  releaseImplementation(deps.leakCanary.release)

  // Chuck
  debugImplementation(deps.chuck.debug)
  releaseImplementation(deps.chuck.release)
}
