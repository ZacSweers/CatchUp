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
  id("com.android.library")
  kotlin("android")
  alias(libs.plugins.redacted)
  alias(libs.plugins.sgp.base)
}

android {
  namespace = "io.sweers.catchup.util"
  buildFeatures {
    androidResources = true
    viewBinding = true
  }
}

redacted {
  redactedAnnotation.set("io.sweers.catchup.util.network.Redacted")
}

dependencies {
  api(project(":libraries:appconfig"))
  api(libs.androidx.annotations)
  api(libs.androidx.coreKtx)
  api(libs.apollo.runtime)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.moshi.core)
  api(libs.okhttp.core)

  implementation(libs.androidx.annotations)
  implementation(libs.androidx.core)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.timber)
  implementation(libs.misc.unbescape)

  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)
}
