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
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.sgp.base)
}

kotlin {
  // region KMP Targets
  androidTarget()
  jvm()
  // endregion

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain {
      dependencies {
        api(libs.androidx.annotations)
        api(libs.anvil.annotations)
        api(libs.compose.runtime)
        api(libs.dagger.runtime)
        api(libs.kotlin.datetime)
        api(libs.kotlinx.immutable)
        api(projects.libraries.di)

        implementation(libs.androidx.annotations)
        implementation(libs.kotlin.coroutinesAndroid)
        implementation(libs.kotlin.datetime)
        implementation(projects.serviceDb)
      }
    }
    with(getByName("androidMain")) {
      dependencies {
        api(libs.androidx.compose.runtime)
      }
    }
  }
}

android { namespace = "catchup.service.api" }

slack {
  features {
    compose()
    dagger()
  }
}
