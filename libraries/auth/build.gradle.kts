/*
 * Copyright (c) 2023 Zac Sweers
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
  kotlin("jvm")
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.moshix)
}

dependencies {
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(libs.retrofit.moshi)
  api(projects.libraries.retrofitconverters)

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.eithernet)
  implementation(libs.kotlin.datetime)
  implementation(libs.okhttp.core)
}
