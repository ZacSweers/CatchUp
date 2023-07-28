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
}

android {
  defaultConfig {
    buildConfigField("String", "IMGUR_CLIENT_ACCESS_TOKEN",
        "\"${project.properties["catchup_imgur_access_token"].toString()}\"")
  }
}

slack {
  features {
    compose()
    moshi(codegen = true)
  }
}

dependencies {
  implementation(projects.libraries.util)
  implementation(libs.moshi.core)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.moshi)
  implementation(libs.okhttp.core)
  implementation(libs.misc.moshiLazyAdapters)

  api(projects.serviceApi)
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
}
