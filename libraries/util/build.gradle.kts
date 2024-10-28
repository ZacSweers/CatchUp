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
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.redacted)
  alias(libs.plugins.foundry.base)
}

android {
  namespace = "catchup.util"
}

redacted {
  redactedAnnotation.set("catchup.util.network.Redacted")
}

foundry {
  android {
    features {
      resources("catchup_util_")
    }
  }
}

dependencies {
  api(libs.androidx.coreKtx)
  api(libs.apollo.api)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.kotlinx.immutable)
  api(libs.moshi.core)
  api(libs.okhttp.core)

  implementation(libs.androidx.core)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.timber)
  implementation(libs.misc.unbescape)
  implementation(projects.libraries.appconfig)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.misc.okio.fakeFileSystem)
  testImplementation(libs.test.junit)
  testImplementation(libs.test.truth)
}
