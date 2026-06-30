/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.android.build.api.variant.BuildConfigField

plugins {
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.ksp)
}

foundry {
  features {
    circuit()
    compose()
    metro()
    moshi(codegen = true)
  }
}

android {
  namespace = "catchup.summarizer"
  buildFeatures { buildConfig = true }
}

sqldelight {
  databases { create("SummarizationsDatabase") { packageName.set("catchup.summarizer") } }
}

androidComponents {
  onVariants {
    it.buildConfigFields?.put(
      "OPEN_AI_KEY",
      BuildConfigField("String", "\"${properties["catchup_openAiKey"]}\"", ""),
    )
  }
}

ksp { arg("circuit.codegen.mode", "METRO") }

dependencies {
  api(project(":libraries:appconfig"))
  api(project(":libraries:di"))
  api(project(":libraries:sqldelight-extensions"))
  api(project(":libraries:util"))
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.circuit.codegenAnnotations)
  api(libs.circuit.runtime)
  api(libs.circuit.runtime.presenter)
  api(libs.circuit.runtime.screen)
  api(libs.circuit.runtime.ui)
  api(libs.okhttp.core)
  api(libs.retrofit.core)

  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(libs.androidx.annotations)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.sqlite)
  implementation(libs.kotlin.coroutines)
  implementation(libs.retrofit.moshi)
}
