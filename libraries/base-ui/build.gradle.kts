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
  api(libs.androidx.annotations)
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.androidx.lifecycle.viewmodel.core)
  api(libs.androidx.lifecycle.viewmodel.savedState)
  api(libs.androidx.palette)
  api(libs.androidx.paletteKtx)
  api(libs.circuit.runtime)
  api(libs.kotlin.coroutines)
  api(libs.kotlin.datetime)

  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.annotations)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.material.ripple)
  implementation(libs.androidx.compose.materialIcons)
  implementation(libs.androidx.compose.uiTooling)
  implementation(libs.androidx.core)
  implementation(libs.kotlin.coroutines)
}
