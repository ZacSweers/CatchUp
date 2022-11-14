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
  kotlin("kapt")
  alias(libs.plugins.anvil)
  alias(libs.plugins.sgp.base)
}


android {
  namespace = "io.sweers.catchup.service.registry"
  buildFeatures {
    androidResources = true
  }
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(project(":service-registry:service-registry-annotations"))
  kapt(libs.dagger.apt.compiler)

  api(project(":services:hackernews"))
  api(project(":services:reddit"))
  api(project(":services:medium"))
  api(project(":services:producthunt"))
//  api(project(":services:imgur"))
  api(project(":services:slashdot"))
  api(project(":services:designernews"))
  api(project(":services:dribbble"))
  api(project(":services:github"))
//  api(project(":services:newsapi"))
  api(project(":services:unsplash"))
  api(project(":services:uplabs"))
}
