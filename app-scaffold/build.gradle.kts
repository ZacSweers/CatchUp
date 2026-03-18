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
import dev.zacsweers.metro.gradle.DelicateMetroGradleApi
import foundry.gradle.DelicateFoundryGradlePluginApi

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.apollo)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.moshix)
  alias(libs.plugins.metro)
}

kotlin { compilerOptions { optIn.add("androidx.compose.material3.ExperimentalMaterial3Api") } }

val enableCompilerReports = project.hasProperty("catchup.enableCompilerReports")

if (enableCompilerReports) {
  metro {
    @OptIn(DelicateMetroGradleApi::class) reportsDestination.set(layout.buildDirectory.dir("metro"))
  }
}

foundry {
  features {
    compose {
      if (enableCompilerReports) {
        val metricsDir = project.layout.buildDirectory.dir("compose_metrics").get().asFile
        @OptIn(DelicateFoundryGradlePluginApi::class) enableCompilerMetricsForDebugging(metricsDir)
      }
    }
    metro()
    moshi(codegen = true)
  }
  // TODO
  //  android {
  //    features {
  //      resources("app")
  //    }
  //  }
}

android {
  defaultConfig {
    buildConfigField(
      "String",
      "GITHUB_DEVELOPER_TOKEN",
      "\"${properties["catchup_github_developer_token"]}\"",
    )
    resValue("string", "changelog_text", "haha")
  }
  androidResources.enable = true
  buildFeatures {
    buildConfig = true
    compose = true
    resValues = true
    viewBinding = true
  }
  buildTypes {
    getByName("debug") {
      buildConfigField(
        "String",
        "IMGUR_CLIENT_ACCESS_TOKEN",
        "\"${project.properties["catchup_imgur_access_token"]}\"",
      )
    }
    getByName("release") {
      buildConfigField("String", "BUGSNAG_KEY", "\"${properties["catchup_bugsnag_key"]}\"")
    }
  }
  namespace = "dev.zacsweers.catchup.app.scaffold"
}

apollo {
  service("github") {
    mapScalar("DateTime", "kotlin.time.Instant")
    mapScalar("URI", "okhttp3.HttpUrl")
    packageName.set("catchup.app.data.github")
    schemaFiles.from(file("src/main/graphql/catchup/app/data/github/schema.json"))
  }
}

ksp { arg("circuit.codegen.mode", "METRO") }

dependencies {
  implementation(project(":bookmarks"))
  implementation(project(":bookmarks:db"))
  implementation(project(":libraries:appconfig"))
  implementation(project(":libraries:auth"))
  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:compose-extensions"))
  implementation(project(":libraries:deeplinking"))
  implementation(project(":libraries:di"))
  implementation(project(":libraries:di:android"))
  implementation(project(":libraries:flowbinding"))
  implementation(project(":libraries:gemoji"))
  implementation(project(":libraries:gemoji:db"))
  implementation(project(":libraries:kotlinutil"))
  implementation(project(":libraries:sqldelight-extensions"))
  implementation(project(":libraries:summarizer"))
  implementation(project(":libraries:unfurler"))
  implementation(project(":libraries:util"))
  implementation(project(":service-api"))
  implementation(project(":service-db"))
  implementation(project(":services:dribbble"))
  implementation(project(":services:github"))
  implementation(project(":services:hackernews"))
  implementation(project(":services:producthunt"))
  implementation(project(":services:reddit"))
  implementation(project(":services:slashdot"))
  implementation(project(":services:unsplash"))
  implementation(project(":services:uplabs"))
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.annotations)
  implementation(libs.androidx.appCompat)
  implementation(libs.androidx.appCompat.resources)
  implementation(libs.androidx.collection)
  implementation(libs.androidx.compose.accompanist.adaptive)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.material.material3.windowSizeClass)
  implementation(libs.androidx.compose.material.ripple)
  implementation(libs.androidx.compose.materialIcons)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.core)
  implementation(libs.androidx.coreKtx)
  implementation(libs.androidx.customTabs)
  implementation(libs.androidx.datastore.core)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.datastore.preferences.core)
  implementation(libs.androidx.design)
  implementation(libs.androidx.emojiAppcompat)
  implementation(libs.androidx.lifecycle.ktx)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.palette)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.preferenceKtx)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.splashscreen)
  implementation(libs.androidx.sqlite)
  implementation(libs.androidx.window)
  implementation(libs.apollo.api)
  implementation(libs.apollo.httpcache)
  implementation(libs.apollo.normalizedCache)
  implementation(libs.apollo.normalizedCache.api)
  implementation(libs.apollo.runtime)
  implementation(libs.circuit.backstack)
  implementation(libs.circuit.codegenAnnotations)
  implementation(libs.circuit.foundation)
  implementation(libs.circuit.overlay)
  implementation(libs.circuit.retained)
  implementation(libs.circuit.runtime)
  implementation(libs.circuit.runtime.presenter)
  implementation(libs.circuit.runtime.screen)
  implementation(libs.circuit.runtime.ui)
  implementation(libs.circuitx.android)
  implementation(libs.circuitx.gestureNav)
  implementation(libs.circuitx.overlays)
  implementation(libs.coil.base)
  implementation(libs.coil.compose)
  implementation(libs.coil.compose.base)
  implementation(libs.coil.default)
  implementation(libs.coil.gif)
  implementation(libs.collapsingToolbar)
  implementation(libs.errorProneAnnotations)
  implementation(libs.fileSize)
  implementation(libs.firebase.core)
  implementation(libs.firebase.database)
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.coroutinesAndroid)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlinx.immutable)
  implementation(libs.markdown)
  implementation(libs.misc.byteunits)
  implementation(libs.misc.composeSettings.base)
  implementation(libs.misc.composeSettings.datastore)
  implementation(libs.misc.debug.processPhoenix)
  implementation(libs.misc.moshiLazyAdapters)
  implementation(libs.misc.okio)
  implementation(libs.misc.tapTargetView)
  implementation(libs.misc.timber)
  implementation(libs.moshi.core)
  implementation(libs.moshi.shimo)
  implementation(libs.okhttp.core)
  implementation(libs.retrofit.core)
  implementation(libs.sqldelight.driver.android)
  implementation(libs.sqldelight.paging)
  implementation(libs.sqldelight.primitiveAdapters)
  implementation(libs.sqldelight.runtime)
  implementation(libs.telephoto.zoomable)
  implementation(libs.telephoto.zoomableImage)
  implementation(libs.telephoto.zoomableImageCoil)
  implementation(libs.xmlutil.serialization)

  releaseImplementation(libs.misc.bugsnag)
  releaseImplementation(libs.misc.leakCanaryObjectWatcherAndroid)

  debugImplementation(project(":libraries:retrofitconverters"))
  debugImplementation(libs.androidx.compose.uiTooling)
  debugImplementation(libs.corbind)
  debugImplementation(libs.misc.debug.guava)
  debugImplementation(libs.misc.debug.soLoader)
  debugImplementation(libs.misc.debug.telescope)
  debugImplementation(libs.misc.leakCanary)
  debugImplementation(libs.misc.leakCanary.shark)
  debugImplementation(libs.misc.leakCanaryObjectWatcherAndroid)
  debugImplementation(libs.okhttp.debug.loggingInterceptor)
  debugImplementation(libs.retrofit.moshi)

  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.misc.okio.fakeFileSystem)
  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)

  ksp(libs.circuit.codegen)
}
