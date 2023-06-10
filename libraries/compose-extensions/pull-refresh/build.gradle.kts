plugins {
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
}

slack { features { compose() } }

dependencies {
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.material3)
  implementation(projects.libraries.composeExtensions)
}
