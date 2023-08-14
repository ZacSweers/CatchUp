/*
 * Copyright (c) 2020 Zac Sweers
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
  alias(libs.plugins.sgp.base)
}

android { namespace = "dev.zacsweers.catchup.compose" }

slack { features { compose() } }

dependencies {
  api(libs.androidx.compose.accompanist.adaptive)
  api(libs.androidx.compose.accompanist.systemUi)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.uiTooling)
  api(libs.kotlin.coroutines)
  api(libs.kotlinx.immutable)
  api(projects.libraries.baseUi)

  implementation(libs.androidx.compose.googleFonts)
  implementation(libs.androidx.compose.material.material3)
}
