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
  kotlin("kapt")
}

android {
  namespace = "io.sweers.catchup.service.slashdot"
  buildFeatures {
    androidResources = true
  }
}

slack {
  features {
    // Because of tikxml
    @Suppress("OPT_IN_USAGE")
    dagger(enableComponents = true)
  }
}

dependencies {
  api(project(":service-api"))
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.rx.java)
  api(libs.tikxml.htmlEscape)

  implementation(project(":libraries:util"))
  implementation(libs.kotlin.datetime)
  implementation(libs.okhttp.core)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.rxJava3)
  implementation(libs.tikxml.annotation)
  implementation(libs.tikxml.core)
  implementation(libs.tikxml.retrofit)

  kapt(libs.tikxml.apt)
}
