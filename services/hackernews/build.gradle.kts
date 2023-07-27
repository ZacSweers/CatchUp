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
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.noarg")
}

android {
  namespace = "catchup.service.hackernews"
  buildFeatures {
    androidResources = true
    resValues = true
    viewBinding = true
  }
}

slack {
  features {
    dagger()
  }
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

noArg {
  annotation("io.sweers.catchup.service.hackernews.model.NoArg")
}

dependencies {
  api(project(":service-api"))
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(projects.libraries.di)

  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:util"))
  implementation(libs.androidx.annotations)
  implementation(libs.firebase.database)
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.datetime)
  implementation(libs.okhttp.core)
  implementation(projects.libraries.kotlinutil)
}
