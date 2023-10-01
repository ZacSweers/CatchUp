plugins {
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

slack { features { compose() } }

dependencies {
  api(libs.androidx.compose.runtime)
  api(libs.androidx.compose.ui)

  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.material3)
}
