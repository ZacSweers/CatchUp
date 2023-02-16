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
  api(project(":libraries:base-ui"))
  api(libs.androidx.annotations)
  api(libs.androidx.compose.uiTooling)
  api(libs.androidx.compose.foundation)
  api(libs.androidx.compose.material)
  api(libs.androidx.compose.material.material3)
  api(libs.zoomables)
  implementation(libs.androidx.compose.googleFonts)
  implementation(libs.androidx.compose.accompanist.systemUi)
}
