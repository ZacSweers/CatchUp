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
}

dependencies {
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.jdk7)
  api(deps.corbind.core)
  api(deps.corbind.androidx.core)
  api(deps.corbind.material)

  api(deps.android.androidx.annotations)
  api(deps.android.androidx.appCompat)
  api(deps.android.androidx.core)
  api(deps.android.androidx.design)
}
