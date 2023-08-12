plugins {
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
}

slack {
  features {
    dagger()
  }
}

dependencies {
  api(libs.circuit.runtime)
  api(libs.kotlinx.immutable)
  api(libs.okhttp.core)
  api(projects.libraries.di)

  implementation(libs.misc.timber)
}