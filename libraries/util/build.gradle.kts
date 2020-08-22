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
  id("dev.zacsweers.redacted.redacted-gradle-plugin")
}

redacted {
  redactedAnnotation = "io.sweers.catchup.util.network.Redacted"
}

dependencies {
  api(deps.android.androidx.annotations)

  implementation(deps.android.androidx.appCompat)
  implementation(deps.android.androidx.core)
  implementation(deps.android.androidx.design)
  implementation(deps.rx.android)
  implementation(deps.kotlin.datetime)

  api(project(":libraries:appconfig"))
  api(deps.android.androidx.coreKtx)
  api(deps.dagger.runtime)
  api(deps.kotlin.stdlib.jdk7)
  api(deps.moshi.core)
  api(deps.misc.timber)
  api(deps.okhttp.core)
  api(deps.rx.java)

  implementation(deps.misc.unbescape)

  testImplementation(deps.test.junit)
  testImplementation(deps.test.truth)
}
