/*
 * Copyright (c) 2019 Zac Sweers
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
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
  alias(libs.plugins.sgp.base)
}

android {
  defaultConfig {
    buildConfigField("String", "SMMRY_API_KEY",
        "\"${properties["catchup_smmry_api_key"]}\"")
  }
  buildFeatures {
    buildConfig = true
    androidResources = true
    viewBinding = true
  }
  namespace = "io.sweers.catchup.smmry"
}

kapt {
  arguments {
    arg("room.schemaLocation", "$projectDir/schemas")
  }
}

slack {
  features {
    dagger()
    moshi(codegen = true) {
      sealed(codegen = true)
    }
  }
}

dependencies {
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.moshi)
  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(libs.misc.lottie)
  implementation(libs.moshi.adapters)
  implementation(libs.moshi.core)
  implementation(libs.moshi.moshix.sealed.runtime)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  kapt(libs.androidx.room.apt)
  implementation(libs.androidx.lifecycle.ktx)
  implementation(libs.kotlin.coroutines)

  api(project(":service-api"))
  api(libs.androidx.annotations)
  api(libs.androidx.appCompat)
  api(libs.androidx.core)
  api(libs.androidx.design)
  api(projects.libraries.di)
}
