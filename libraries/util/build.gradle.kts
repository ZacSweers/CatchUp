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
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kmp)
  alias(libs.plugins.redacted)
  alias(libs.plugins.foundry.base)
}

kotlin {
  // region KMP Targets
  android { namespace = "catchup.util" }
  jvm()
  // endregion

  sourceSets {
    commonMain {
      dependencies {
        api(libs.apollo.api)
        api(libs.kotlin.datetime)
        api(libs.kotlinx.immutable)
        api(libs.metro.runtime)
        api(libs.moshi.core)
        api(libs.okhttp.core)

        implementation(project(":libraries:appconfig"))
        implementation(libs.androidx.annotations)
        implementation(libs.misc.unbescape)
      }
    }
    commonTest { dependencies { implementation(libs.kotlin.test) } }
    androidMain {
      dependencies {
        api(libs.androidx.coreKtx)

        implementation(libs.androidx.core)
        implementation(libs.misc.timber)
      }
    }
    jvmMain { dependencies { api(libs.misc.okio) } }
    jvmTest {
      dependencies {
        api(libs.misc.okio)

        implementation(libs.misc.okio.fakeFileSystem)
        implementation(libs.test.junit)
        implementation(libs.test.truth)
      }
    }
  }
}

redacted { redactedAnnotations.add("catchup/util/network/Redacted") }

foundry { android { features { resources("catchup_util_") } } }
