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
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
}

android {
  namespace = "catchup.ui.core"
  buildFeatures { androidResources = true }
}

slack {
  features {
    compose()
    dagger()
  }
}

dependencies {
  api(project(":libraries:appconfig"))
  api(project(":libraries:di"))
  api(project(":libraries:util"))
  api(libs.androidx.activity)
  api(libs.androidx.annotations)
  api(libs.androidx.appCompat)
  api(libs.androidx.compose.material.material3)
  api(libs.androidx.compose.runtime)
  api(libs.androidx.core)
  api(libs.androidx.design)
  api(libs.androidx.fragment)
  api(libs.androidx.paging.compose)
  api(libs.androidx.palette)
  api(libs.androidx.paletteKtx)
  api(libs.autodispose.android)
  api(libs.autodispose.androidxLifecycle)
  api(libs.autodispose.core)
  api(libs.autodispose.lifecycle)
  api(libs.circuit.foundation)
  api(libs.kotlin.coroutines)
  api(libs.kotlin.datetime)
  api(libs.rx.java)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.uiTooling)
  implementation(libs.rx.relay)
}
