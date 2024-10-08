import com.android.build.api.variant.BuildConfigField

plugins {
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.ksp)
  alias(libs.plugins.sqldelight)
}

foundry {
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
  api(libs.circuit.runtime.screen)
  api(libs.circuit.runtime.ui)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(projects.libraries.appconfig)
  api(projects.libraries.di)
  api(projects.libraries.sqldelightExtensions)
  api(projects.libraries.util)

  implementation(libs.androidx.annotations)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.sqlite)
  implementation(libs.kotlin.coroutines)
  implementation(libs.retrofit.moshi)
  implementation(projects.libraries.baseUi)
  implementation(projects.libraries.retrofitconverters)
}
