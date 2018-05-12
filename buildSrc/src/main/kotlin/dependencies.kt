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

@file:Suppress("ClassName", "unused")

import org.gradle.api.Project
import java.io.File

fun String?.letIfEmpty(fallback: String): String {
  return if (this == null || isEmpty()) {
    fallback
  } else {
    this
  }
}

fun String?.execute(workingDir: File, fallback: String): String {
  Runtime.getRuntime().exec(this, null, workingDir).let {
    it.waitFor()
    return try {
      it.inputStream.reader().readText().trim().letIfEmpty(fallback)
    } catch (e: Exception) {
      fallback
    }
  }
}

object deps {
  object versions {
    const val androidTestSupport = "1.1.0-alpha1"
    const val apollo = "0.5.0"
    const val autodispose = "0.8.0"
    const val barber = "1.3.1"
    const val butterknife = "8.8.1"
    const val chuck = "1.1.0"
    const val conductor = "2.1.4"
    const val crumb = "0.0.1"
    const val dagger = "2.16"
    const val errorProne = "2.3.1"
    const val espresso = "3.1.0-alpha1"
    const val firebase = "12.0.1"
    const val glide = "4.7.1"
    const val hyperion = "0.9.23"
    const val inspector = "0.3.0"
    const val jetpack = "1.0.0-alpha1"
    const val jetpackArch = "2.0.0-alpha1"
    const val kotlin = "1.2.41"
    const val leakcanary = "1.5.4"
    const val moshi = "1.6.0-RC1"
    const val okhttp = "3.10.0"
    const val playServices = firebase
    const val retrofit = "2.4.0"
    const val rxbinding = "2.1.1"
    const val rxpalette = "0.3.0"
    const val stetho = "1.5.0"
    const val tikxml = "0.8.13"
  }

  object android {
    object arch {
      object lifecycle {
        const val apt = "androidx.lifecycle:lifecycle-compiler:${versions.jetpackArch}"
        const val extensions = "androidx.lifecycle:lifecycle-extensions:${versions.jetpackArch}"
      }

      object room {
        const val apt = "androidx.room:room-compiler:${versions.jetpackArch}"
        const val runtime = "androidx.room:room-runtime:${versions.jetpackArch}"
        const val rxJava2 = "androidx.room:room-rxjava2:${versions.jetpackArch}"
      }
    }

    object build {
      const val buildToolsVersion = "27.0.3"
      const val compileSdkVersion = "android-P"
      const val minSdkVersion = 21
      const val targetSdkVersion = "P"
    }

    object firebase {
      const val core = "com.google.firebase:firebase-core:${versions.firebase}"
      const val config = "com.google.firebase:firebase-config:${versions.firebase}"
      const val database = "com.google.firebase:firebase-database:${versions.firebase}"
      const val gradlePlugin = "com.google.firebase:firebase-plugins:1.1.5"
      const val perf = "com.google.firebase:firebase-perf:${versions.firebase}"
    }

    const val gradlePlugin = "com.android.tools.build:gradle:3.2.0-alpha14"
    const val ktx = "androidx.core:core-ktx:1.0.0-alpha1"

    object support {
      const val annotations = "androidx.annotation:annotation:${versions.jetpack}"
      const val legacyAnnotations = "com.android.support:support-annotations:28.0.0-alpha1"
      const val appCompat = "androidx.appcompat:appcompat:${versions.jetpack}"
      const val cardView = "androidx.cardview:cardview:${versions.jetpack}"
      const val constraintLayout = "androidx.constraintlayout:constraintlayout:1.1.0"
      const val customTabs = "androidx.browser:browser:${versions.jetpack}"
      const val design = "com.google.android.material:material:${versions.jetpack}"
      const val drawerLayout = "androidx.drawerlayout:drawerlayout:${versions.jetpack}"
      const val viewPager = "androidx.viewpager:viewpager:${versions.jetpack}"
      const val swipeRefresh = "androidx.swiperefreshlayout:swiperefreshlayout:${versions.jetpack}"
      const val palette = "androidx.palette:palette:${versions.jetpack}"
      const val percent = "androidx.percentlayout:percentlayout:${versions.jetpack}"
      const val recyclerView = "androidx.recyclerview:recyclerview:${versions.jetpack}"
      const val compat = "androidx.core:core:${versions.jetpack}"
    }
  }

  object apollo {
    const val androidSupport = "com.apollographql.apollo:apollo-android-support:${versions.apollo}"
    const val gradlePlugin = "com.apollographql.apollo:apollo-gradle-plugin:${versions.apollo}"
    const val httpcache = "com.apollographql.apollo:apollo-http-cache:${versions.apollo}"
    const val runtime = "com.apollographql.apollo:apollo-runtime:${versions.apollo}"
    const val rx2Support = "com.apollographql.apollo:apollo-rx2-support:${versions.apollo}"
  }

  object auto {
    const val common = "com.google.auto:auto-common:0.10"
    const val service = "com.google.auto.service:auto-service:1.0-rc4"
  }

  object autoDispose {
    const val core = "com.uber.autodispose:autodispose:${versions.autodispose}"
    const val android = "com.uber.autodispose:autodispose-android:${versions.autodispose}"
    const val kotlin = "com.uber.autodispose:autodispose-kotlin:${versions.autodispose}"
  }

  object barber {
    const val apt = "io.sweers.barber:barber-compiler:${versions.barber}"
    const val api = "io.sweers.barber:barber-api:${versions.barber}"
  }

  object build {
    val ci = "true" == System.getenv("CI")

    fun gitSha(project: Project): String {
      // query git for the SHA, Tag and commit count. Use these to automate versioning.
      return "git rev-parse --short HEAD".execute(project.rootDir, "none")
    }

    fun gitTag(project: Project): String {
      return "git describe --tags".execute(project.rootDir, "dev")
    }

    fun gitCommitCount(project: Project): Int {
      return 100 + "git rev-list --count HEAD".execute(project.rootDir, "0").toInt()
    }

    fun gitTimestamp(project: Project): Int {
      return "git log -n 1 --format=%at".execute(project.rootDir, "0").toInt()
    }

    object gradlePlugins {
      const val bugsnag = "com.bugsnag:bugsnag-android-gradle-plugin:3.2.6"
      const val playPublisher = "com.github.triplet.gradle:play-publisher:1.2.1-beta1"
      const val psync = "io.sweers.psync:psync:2.0.0-20171017.111936-4"
      const val versions = "com.github.ben-manes:gradle-versions-plugin:0.17.0"
    }

    object repositories {
      const val google = "https://maven.google.com"
      const val jitpack = "https://jitpack.io"
      const val kotlineap = "https://dl.bintray.com/kotlin/kotlin-eap"
      const val plugins = "https://plugins.gradle.org/m2/"
      const val snapshots = "https://oss.sonatype.org/content/repositories/snapshots/"
    }

    const val javapoet = "com.squareup:javapoet:1.11.0"
  }

  object butterKnife {
    const val apt = "com.jakewharton:butterknife-compiler:${versions.butterknife}"
    const val runtime = "com.jakewharton:butterknife:${versions.butterknife}"
  }

  object chuck {
    const val debug = "com.readystatesoftware.chuck:library:${versions.chuck}"
    const val release = "com.readystatesoftware.chuck:library-no-op:${versions.chuck}"
  }

  object conductor {
    const val core = "com.bluelinelabs:conductor:${versions.conductor}"
    const val autoDispose = "com.bluelinelabs:conductor-autodispose:${versions.conductor}"
    const val support = "com.bluelinelabs:conductor-support:${versions.conductor}"
  }

  object crumb {
    const val annotations = "com.uber.crumb:crumb-annotations:${versions.crumb}"
    const val compiler = "com.uber.crumb:crumb-compiler:${versions.crumb}"
    const val compilerApi = "com.uber.crumb:crumb-compiler-api:${versions.crumb}"
  }

  object dagger {
    object android {
      object apt {
        const val processor = "com.google.dagger:dagger-android-processor:${versions.dagger}"
      }

      const val runtime = "com.google.dagger:dagger-android:${versions.dagger}"
    }

    object apt {
      const val compiler = "com.google.dagger:dagger-compiler:${versions.dagger}"
    }

    const val runtime = "com.google.dagger:dagger:${versions.dagger}"
    const val spi = "com.google.dagger:dagger-spi:${versions.dagger}"
  }

  object errorProne {
    object build {
      const val core = "com.google.errorprone:error_prone_core:${versions.errorProne}"
    }

    object compileOnly {
      const val annotations = "com.google.errorprone:error_prone_annotations:${versions.errorProne}"
    }

    const val gradlePlugin = "net.ltgt.gradle:gradle-errorprone-plugin:0.0.14"
  }

  object glide {
    object apt {
      const val compiler = "com.github.bumptech.glide:compiler:${versions.glide}"
    }

    const val annotations = "com.github.bumptech.glide:annotations:${versions.glide}"
    const val core = "com.github.bumptech.glide:glide:${versions.glide}"
    const val okhttp = "com.github.bumptech.glide:okhttp3-integration:${versions.glide}"
    const val recyclerView = "com.github.bumptech.glide:recyclerview-integration:${versions.glide}"
  }

  object hyperion {
    object core {
      const val debug = "com.willowtreeapps.hyperion:hyperion-core:${versions.hyperion}"
      const val release = "com.willowtreeapps.hyperion:hyperion-core-no-op:${versions.hyperion}"
    }
    object plugins {
      const val appInfo = "com.star_zero:hyperion-appinfo:1.0.0"
      const val attr = "com.willowtreeapps.hyperion:hyperion-attr:${versions.hyperion}"
      const val chuck = "com.github.Commit451:Hyperion-Chuck:1.0.0"
      const val crash = "com.willowtreeapps.hyperion:hyperion-crash:${versions.hyperion}"
      const val disk = "com.willowtreeapps.hyperion:hyperion-disk:${versions.hyperion}"
      const val geigerCounter = "com.willowtreeapps.hyperion:hyperion-geiger-counter:${versions.hyperion}"
      const val measurement = "com.willowtreeapps.hyperion:hyperion-measurement:${versions.hyperion}"
      const val phoenix = "com.willowtreeapps.hyperion:hyperion-phoenix:${versions.hyperion}"
      const val recorder = "com.willowtreeapps.hyperion:hyperion-recorder:${versions.hyperion}"
      const val sharedPreferences = "com.willowtreeapps.hyperion:hyperion-shared-preferences:${versions.hyperion}"
    }
  }

  object inspector {
    object apt {
      const val compiler = "io.sweers.inspector:inspector-compiler:${versions.inspector}"

      object extensions {
        const val android = "io.sweers.inspector:inspector-android-compiler-extension:${versions.inspector}"
        const val autovalue = "io.sweers.inspector:inspector-autovalue-compiler-extension:${versions.inspector}"
        const val nullability = "io.sweers.inspector:inspector-nullability-compiler-extension:${versions.inspector}"
      }
    }

    const val core = "io.sweers.inspector:inspector:${versions.inspector}"

    object factoryCompiler {
      const val apt = "io.sweers.inspector:inspector-factory-compiler:${versions.inspector}"

      object compileOnly {
        const val annotations = "io.sweers.inspector:inspector-factory-compiler-annotations:${versions.inspector}"
      }
    }

  }

  object kotlin {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
    const val metadata = "me.eugeniomarletti.kotlin.metadata:kotlin-metadata:1.4.0"
    const val noArgGradlePlugin = "org.jetbrains.kotlin:kotlin-noarg:${versions.kotlin}"
    const val poet = "com.squareup:kotlinpoet:0.7.0"

    object stdlib {
      const val core = "org.jetbrains.kotlin:kotlin-stdlib:${versions.kotlin}"
      const val jdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${versions.kotlin}"
      const val jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}"
    }
  }

  object leakCanary {
    const val debug = "com.squareup.leakcanary:leakcanary-android:${versions.leakcanary}"
    const val release = "com.squareup.leakcanary:leakcanary-android-no-op:${versions.leakcanary}"
  }

  object misc {
    const val bugsnag = "com.bugsnag:bugsnag-android:4.3.4"

    object debug {
      const val madge = "com.jakewharton.madge:madge:1.1.4"
      const val processPhoenix = "com.jakewharton:process-phoenix:2.0.0"
      const val scalpel = "com.jakewharton.scalpel:scalpel:1.1.2"
      const val telescope = "com.mattprecious.telescope:telescope:2.1.0"
    }

    const val javaxInject = "org.glassfish:javax.annotation:10.0-b28"
    const val jsoup = "org.jsoup:jsoup:1.11.3"
    const val jsr305 = "com.google.code.findbugs:jsr305:3.0.2"
    const val lazythreeten = "com.gabrielittner.threetenbp:lazythreetenbp:0.3.0"
    const val lottie = "com.airbnb.android:lottie:2.5.5"
    const val moshiLazyAdapters = "com.serjltt.moshi:moshi-lazy-adapters:2.2"
    const val okio = "com.squareup.okio:okio:1.14.0"
    const val recyclerViewAnimators = "jp.wasabeef:recyclerview-animators:2.3.0"
    const val tapTargetView = "com.getkeepsafe.taptargetview:taptargetview:1.11.0"
    const val timber = "com.jakewharton.timber:timber:4.7.0"
    const val unbescape = "org.unbescape:unbescape:1.1.6.RELEASE"
  }

  object moshi {
    const val core = "com.squareup.moshi:moshi:${versions.moshi}"
    const val compiler = "com.squareup.moshi:moshi-kotlin-codegen-compiler:${versions.moshi}"
  }

  object okhttp {
    const val core = "com.squareup.okhttp3:okhttp:${versions.okhttp}"

    object debug {
      const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${versions.okhttp}"
    }

    const val webSockets = "com.squareup.okhttp3:okhttp-ws:${versions.okhttp}"
  }

  object retrofit {
    const val core = "com.squareup.retrofit2:retrofit:${versions.retrofit}"

    object debug {
      const val mock = "com.squareup.retrofit2:retrofit-mock:${versions.retrofit}"
    }

    const val moshi = "com.squareup.retrofit2:converter-moshi:${versions.retrofit}"
    const val rxJava2 = "com.squareup.retrofit2:adapter-rxjava2:${versions.retrofit}"
  }

  object rx {
    const val android = "io.reactivex.rxjava2:rxandroid:2.0.2"

    object binding {
      const val core = "com.jakewharton.rxbinding2:rxbinding-kotlin:${versions.rxbinding}"
      const val v4 = "com.jakewharton.rxbinding2:rxbinding-support-v4-kotlin:${versions.rxbinding}"
      const val design = "com.jakewharton.rxbinding2:rxbinding-design-kotlin:${versions.rxbinding}"
    }

    const val java = "io.reactivex.rxjava2:rxjava:2.1.13"

    object palette {
      const val core = "io.sweers.rxpalette:rxpalette:${versions.rxpalette}"
      const val kotlin = "io.sweers.rxpalette:rxpalette-kotlin:${versions.rxpalette}"
    }

    const val preferences = "com.f2prateek.rx.preferences2:rx-preferences:2.0.0"
    const val relay = "com.jakewharton.rxrelay2:rxrelay:2.0.0"
  }

  object stetho {
    object debug {
      const val core = "com.facebook.stetho:stetho:${versions.stetho}"
      const val okhttp = "com.facebook.stetho:stetho-okhttp3:${versions.stetho}"
      const val timber = "com.facebook.stetho:stetho-timber:${versions.stetho}"
    }
  }

  object tikxml {
    const val annotation = "com.tickaroo.tikxml:annotation:${versions.tikxml}"
    const val apt = "com.tickaroo.tikxml:processor:${versions.tikxml}"
    const val core = "com.tickaroo.tikxml:core:${versions.tikxml}"
    const val htmlEscape = "com.tickaroo.tikxml:converter-htmlescape:${versions.tikxml}"
    const val retrofit = "com.tickaroo.tikxml:retrofit-converter:${versions.tikxml}"
  }

  object test {
    object android {
      object espresso {
        const val core = "androidx.test.espresso:espresso-core:${versions.espresso}"
        const val contrib = "androidx.test.espresso:espresso-contrib:${versions.espresso}"
        const val web = "androidx.test.espresso:espresso-web:${versions.espresso}"
      }

      const val runner = "androidx.test:runner:${versions.androidTestSupport}"
      const val rules = "androidx.test:rules:${versions.androidTestSupport}"
    }

    const val junit = "junit:junit:4.12"
    const val robolectric = "org.robolectric:robolectric:4.0-alpha-1"
    const val truth = "com.google.truth:truth:0.40"
  }
}
