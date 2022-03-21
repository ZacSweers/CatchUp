/*
 * Copyright (c) 2019 Zac Sweers
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
}

dependencies {
  kapt(deps.dagger.apt.compiler)
  compileOnly(deps.misc.javaxInject)
  api(project(":libraries:appconfig"))
  api(project(":libraries:util"))
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.jdk7)
  api(deps.autoDispose.core)
  api(deps.autoDispose.android)
  api(deps.autoDispose.lifecycle)
  api(deps.autoDispose.androidxLifecycle)
  api(deps.rx.java)
  implementation(deps.rx.relay)
  api(deps.android.androidx.annotations)
  api(deps.android.androidx.activity)
  api(deps.android.androidx.appCompat)
  api(deps.android.androidx.core)
  api(deps.android.androidx.design)
  api(deps.android.androidx.fragment)
  api(deps.android.androidx.palette)
  api(deps.android.androidx.paletteKtx)
}
android {
  namespace = "catchup.ui.core"
}
