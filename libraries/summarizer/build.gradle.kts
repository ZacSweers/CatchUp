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
  namespace = "dev.zacsweers.catchup.summarizer"
  buildFeatures { buildConfig = true }
}

sqldelight {
  databases {
    create("SummarizationsDatabase") {
      packageName.set("dev.zacsweers.catchup.summarizer")
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
  implementation(libs.androidx.compose.material.material3)
  implementation(libs.androidx.compose.ui)
  ksp(libs.circuit.codegen)
  implementation(libs.circuit.codegenAnnotations)
  implementation(libs.circuit.core)
  implementation(libs.circuit.overlay)
  implementation(libs.circuit.retained)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.moshi)
  implementation(libs.sqldelight.driver.android)
  implementation(libs.androidx.compose.accompanist.systemUi)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(projects.libraries.di)
  implementation(projects.libraries.util)
  implementation(projects.libraries.retrofitconverters)
  implementation(projects.libraries.composeExtensions)
}
