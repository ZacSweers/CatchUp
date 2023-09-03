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
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.moshix)
  // TODO https://github.com/slackhq/slack-gradle-plugin/issues/559
  id("com.android.lint")
}

kotlin {
  // region KMP Targets
  jvm()
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) targetHierarchy.default()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.androidx.annotations)
        api(libs.dagger.runtime)
        api(libs.misc.okio)
        api(libs.okhttp.core)

        implementation(libs.androidx.datastore.preferences.core)
        implementation(libs.eithernet)
        implementation(libs.kotlin.coroutines)
        implementation(libs.kotlin.datetime)
        implementation(libs.okhttp.core)
        implementation(libs.retrofit.core)
        implementation(libs.retrofit.moshi)
}
    }
  }
}
