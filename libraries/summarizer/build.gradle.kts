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
    circuit(codegen = true)
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
