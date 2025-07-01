plugins {
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

foundry {
  features {
    metro()
  }
}

dependencies {
  api(project(":libraries:di"))
  api(libs.circuit.runtime.screen)
  api(libs.kotlinx.immutable)
  api(libs.okhttp.core)

  implementation(libs.misc.timber)
}
