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
  alias(libs.plugins.ksp)
  alias(libs.plugins.anvil)
  alias(libs.plugins.sgp.base)
}

anvil {
  generateDaggerFactories.set(true)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(project(":libraries:util"))

  ksp(libs.androidx.room.apt)
  compileOnly(libs.misc.jsr250)

  api(libs.androidx.room.runtime)
  api(libs.dagger.runtime)
  api(libs.moshi.core)

  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)
}
android {
  namespace = "io.sweers.catchup.gemoji"
}
