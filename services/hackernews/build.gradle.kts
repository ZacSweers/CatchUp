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
import com.android.build.api.variant.LibraryAndroidComponentsExtension

plugins {
  id("com.android.library")
  kotlin("android")
  id(deps.anvil.pluginId)
  id("kotlin-noarg")
  id("dev.zacsweers.moshix")
}

configure<LibraryAndroidComponentsExtension> {
  onVariants { variant ->
    // Configure firebase
    fun firebaseProperty(property: String, resolveName: Boolean = true) {
      val buildTypeName = variant.buildType!!
      val name = if (resolveName && buildTypeName == "debug") {
        "$property.debug"
      } else property
      val value = project.properties[name].toString()
      variant.resValues.put(variant.makeResValueKey("string", property.removePrefix("catchup.")),
        com.android.build.api.variant.ResValue(value))
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

android {
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = deps.android.androidx.compose.compilerVersion
  }
}

noArg {
  annotation("io.sweers.catchup.service.hackernews.model.NoArg")
}

dependencies {
  api(project(":service-api"))
  implementation(project(":libraries:util"))
  implementation(project(":libraries:base-ui"))

  implementation(deps.android.androidx.compose.uiTooling)
  implementation(deps.android.androidx.compose.foundation)
  implementation(deps.android.androidx.compose.material)
  implementation(deps.android.androidx.swipeRefresh)
  implementation(deps.android.androidx.viewModel.core)
  implementation(deps.android.androidx.viewModel.ktx)
  implementation(deps.android.androidx.viewModel.savedState)
  implementation(deps.android.androidx.constraintLayout)
  implementation(deps.android.firebase.database)
  implementation(deps.retrofit.core)
  implementation(deps.retrofit.moshi)
  implementation(deps.moshi.core)
  implementation(deps.coil.default)
  implementation(deps.kotlin.datetime)

  api(deps.android.androidx.design)
  api(deps.android.androidx.fragmentKtx)
  api(deps.android.androidx.annotations)
  api(deps.dagger.runtime)
  api(deps.rx.java)
}
