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
  alias(libs.plugins.ksp)
}

android { namespace = "io.sweers.catchup.service" }

slack {
  features {
    compose()
    dagger()
  }
}

ksp {
  arg("room.generateKotlin", "true")
}

dependencies {
  ksp(libs.androidx.room.apt)

  api(project(":libraries:appconfig"))
  api(project(":libraries:gemoji"))
  api(project(":libraries:retrofitconverters"))
  api(libs.androidx.annotations)
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.androidx.room.runtime)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(projects.libraries.di)

  implementation(libs.androidx.annotations)
  implementation(libs.kotlin.coroutinesAndroid)
  implementation(libs.kotlin.datetime)
}
