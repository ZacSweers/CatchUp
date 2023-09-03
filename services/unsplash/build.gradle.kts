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

plugins {
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
}

android {
  defaultConfig {
    buildConfigField("String", "UNSPLASH_API_KEY",
        "\"${project.properties["catchup_unsplash_api_key"]}\"")
  }
  buildFeatures {
    buildConfig = true
  }
  namespace = "dev.zacsweers.catchup.services.unsplash"
}

slack {
  features {
    dagger()
    moshi(codegen = true)
  }
  android { features { resources("catchup_service_unsplash_") } }
}

dependencies {
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(projects.libraries.appconfig)
  api(projects.libraries.di)
  api(projects.serviceApi)

  implementation(libs.androidx.annotations)
  implementation(libs.kotlin.datetime)
  implementation(libs.okhttp.core)
  implementation(libs.retrofit.moshi)
  implementation(projects.libraries.retrofitconverters)
  implementation(projects.libraries.util)
}
