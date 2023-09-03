import com.android.build.api.variant.BuildConfigField

plugins {
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
  kotlin("plugin.parcelize")
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
}

slack {
  features {
    compose()
    dagger()
    moshi(codegen = true)
  }
}

android {
  namespace = "catchup.summarizer"
  buildFeatures { buildConfig = true }
}

sqldelight {
  databases {
    create("SummarizationsDatabase") {
      packageName.set("catchup.summarizer")
    }
  }
}

androidComponents {
  onVariants {
    it.buildConfigFields.put(
      "OPEN_AI_KEY",
      BuildConfigField("String", "\"${properties["catchup_openAiKey"]}\"", "")
    )
  }
}

dependencies {
  ksp(libs.circuit.codegen)

  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)
  api(libs.circuit.codegenAnnotations)
  api(libs.circuit.runtime)
  api(libs.circuit.runtime.presenter)
  api(libs.circuit.runtime.ui)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(projects.libraries.appconfig)
  api(projects.libraries.di)
  api(projects.libraries.util)

  implementation(libs.androidx.annotations)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.kotlin.coroutines)
  implementation(libs.retrofit.moshi)
  implementation(libs.sqldelight.driver.android)
  implementation(projects.libraries.baseUi)
  implementation(projects.libraries.retrofitconverters)
}
