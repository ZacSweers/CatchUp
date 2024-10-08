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
}

android {
  namespace = "catchup.gemoji"
}

foundry {
  features {
    dagger()
  }
}

dependencies {
  api(libs.dagger.runtime)
  api(projects.libraries.di)
  api(projects.libraries.gemoji.db)

  implementation(libs.androidx.annotations)
  implementation(libs.androidx.sqlite)
  implementation(libs.androidx.sqlite.framework)
  implementation(libs.kotlin.coroutines)
  implementation(libs.misc.timber)
  implementation(libs.sqldelight.coroutines)
  implementation(libs.sqldelight.driver.android)
  implementation(libs.sqldelight.runtime)
  implementation(projects.libraries.util)

  compileOnly(libs.misc.jsr250)

  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)
}
