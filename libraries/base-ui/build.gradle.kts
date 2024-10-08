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
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "catchup.ui.core"
}

foundry {
  features {
    compose()
    dagger()
  }
  android {
    features {
      resources("catchup_baseui_")
    }
  }
}

dependencies {
  api(libs.androidx.annotations)
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.androidx.palette)
  api(libs.androidx.paletteKtx)
  api(libs.circuit.runtime)
  api(libs.kotlin.coroutines)
  api(projects.libraries.di)

  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.annotations)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.materialIcons)
  implementation(libs.androidx.compose.uiTooling)
  implementation(libs.androidx.core)
  implementation(libs.haze)
  implementation(libs.kotlin.coroutines)
  implementation(projects.libraries.appconfig)
  implementation(projects.libraries.util)
}
