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
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
}

android { namespace = "catchup.service.slashdot" }

foundry {
  features { dagger() }
  android { features { resources("catchup_service_sd_") } }
}

dependencies {
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.kotlinx.serialization.core)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(libs.xmlutil.serialization)
  api(projects.libraries.appconfig)
  api(projects.libraries.di)
  api(projects.serviceApi)

  implementation(libs.kotlin.datetime)
  implementation(libs.okhttp.core)
  implementation(libs.retrofit.kotlinxSerialization)
  implementation(libs.tikxml.htmlEscape)
  implementation(projects.libraries.retrofitconverters)
  implementation(projects.libraries.util)

  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)
}
