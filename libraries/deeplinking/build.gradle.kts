plugins {
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

slack {
  features {
    dagger()
  }
}

dependencies {
  api(libs.circuit.runtime.screen)
  api(libs.kotlinx.immutable)
  api(libs.okhttp.core)
  api(projects.libraries.di)

  implementation(libs.misc.timber)
}
