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
    const val androidTestSupport = "1.0.1"
    const val apollo = "0.4.4"
    const val archComponentsLifecycle = "1.1.0"
    const val archComponentsRoom = "1.1.0-alpha3"
    const val autodispose = "0.6.1"
    const val barber = "1.3.1"
    const val butterknife = "8.8.1"
    const val chuck = "1.1.0"
    const val conductor = "2.1.4"
    const val dagger = "2.14.1"
    const val errorProne = "2.2.0"
    const val espresso = "3.0.1"
    const val firebase = "11.8.0"
    const val glide = "4.6.1"
    const val inspector = "0.3.0"
    const val kotlin = "1.2.30"
    const val leakcanary = "1.5.4"
    const val okhttp = "3.10.0"
    const val playServices = "11.8.0"
    const val retrofit = "2.3.0"
    const val rxbinding = "2.1.1"
    const val rxpalette = "0.3.0"
    const val stetho = "1.5.0"
    const val support = "27.1.0"
    const val tikxml = "0.8.13"
  }

  object android {
    object arch {
      object lifecycle {
        const val apt = "android.arch.lifecycle:compiler:${versions.archComponentsLifecycle}"
        const val extensions = "android.arch.lifecycle:extensions:${versions.archComponentsLifecycle}"
      }

      object room {
        const val apt = "android.arch.persistence.room:compiler:${versions.archComponentsRoom}"
        const val runtime = "android.arch.persistence.room:runtime:${versions.archComponentsRoom}"
        const val rxJava2 = "android.arch.persistence.room:rxjava2:${versions.archComponentsRoom}"
      }
    }

    object build {
      const val buildToolsVersion = "27.0.3"
      const val compileSdkVersion = 27
      const val minSdkVersion = 21
      const val targetSdkVersion = 27
    }

    object firebase {
      const val core = "com.google.firebase:firebase-core:${versions.firebase}"
      const val config = "com.google.firebase:firebase-config:${versions.firebase}"
      const val database = "com.google.firebase:firebase-database:${versions.firebase}"
      const val gradlePlugin = "com.google.firebase:firebase-plugins:1.1.5"
      const val perf = "com.google.firebase:firebase-perf:${versions.firebase}"
    }

    const val gradlePlugin = "com.android.tools.build:gradle:3.2.0-alpha05"
    const val ktx = "androidx.core:core-ktx:0.2"

    object support {
      const val annotations = "com.android.support:support-annotations:${versions.support}"
      const val appCompat = "com.android.support:appcompat-v7:${versions.support}"
      const val cardView = "com.android.support:cardview-v7:${versions.support}"
      const val constraintLayout = "com.android.support.constraint:constraint-layout:1.1.0-beta5"
      const val customTabs = "com.android.support:customtabs:${versions.support}"
      const val design = "com.android.support:design:${versions.support}"
      const val multidex = "com.android.support:multidex:1.0.2"
      const val palette = "com.android.support:palette-v7:${versions.support}"
      const val percent = "com.android.support:percent:${versions.support}"
      const val recyclerView = "com.android.support:recyclerview-v7:${versions.support}"
      const val compat = "com.android.support:support-compat:${versions.support}"
      const val v4 = "com.android.support:support-v4:${versions.support}"
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
      const val bugsnag = "com.bugsnag:bugsnag-android-gradle-plugin:3.2.5"
      const val playPublisher = "com.github.triplet.gradle:play-publisher:1.2.0"
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
  }

  object errorProne {
    object build {
      const val core = "com.google.errorprone:error_prone_core:${versions.errorProne}"
    }

    object compileOnly {
      const val annotations = "com.google.errorprone:error_prone_annotations:${versions.errorProne}"
    }

    const val gradlePlugin = "net.ltgt.gradle:gradle-errorprone-plugin:0.0.13"
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
    const val metadata = "me.eugeniomarletti:kotlin-metadata:1.2.1"
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
    const val bugsnag = "com.bugsnag:bugsnag-android:4.3.1"

    object debug {
      const val madge = "com.jakewharton.madge:madge:1.1.4"
      const val processPhoenix = "com.jakewharton:process-phoenix:2.0.0"
      const val scalpel = "com.jakewharton.scalpel:scalpel:1.1.2"
      const val telescope = "com.mattprecious.telescope:telescope:2.1.0"
    }

    const val guava = "com.google.guava:guava:24.0-android"
    const val javaxInject = "org.glassfish:javax.annotation:10.0-b28"
    const val jsr305 = "com.google.code.findbugs:jsr305:3.0.2"
    const val lazythreeten = "com.gabrielittner.threetenbp:lazythreetenbp:0.2.0"
    const val lottie = "com.airbnb.android:lottie:2.5.0"
    const val moshi = "com.squareup.moshi:moshi:1.5.0"
    const val moshiLazyAdapters = "com.serjltt.moshi:moshi-lazy-adapters:2.1"
    const val okio = "com.squareup.okio:okio:1.14.0"
    const val recyclerViewAnimators = "jp.wasabeef:recyclerview-animators:2.3.0"
    const val tapTargetView = "com.getkeepsafe.taptargetview:taptargetview:1.11.0"
    const val timber = "com.jakewharton.timber:timber:4.6.1"
    const val unbescape = "org.unbescape:unbescape:1.1.5.RELEASE"
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

    const val java = "io.reactivex.rxjava2:rxjava:2.1.10"

    object palette {
      const val core = "io.sweers.rxpalette:rxpalette:${versions.rxpalette}"
      const val kotlin = "io.sweers.rxpalette:rxpalette-kotlin:${versions.rxpalette}"
    }

    const val preferences = "com.f2prateek.rx.preferences2:rx-preferences:2.0.0-RC3"
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
        const val core = "com.android.support.test.espresso:espresso-core:${versions.espresso}"
        const val contrib = "com.android.support.test.espresso:espresso-contrib:${versions.espresso}"
        const val web = "com.android.support.test.espresso:espresso-web:${versions.espresso}"
      }

      const val runner = "com.android.support.test:runner:${versions.androidTestSupport}"
      const val rules = "com.android.support.test:rules:${versions.androidTestSupport}"
    }

    const val junit = "junit:junit:4.12"
    const val robolectric = "org.robolectric:robolectric:3.2.2"
    const val truth = "com.google.truth:truth:0.39"
  }
}
