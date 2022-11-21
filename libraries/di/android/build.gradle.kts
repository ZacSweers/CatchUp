plugins {
  id("com.android.library")
  kotlin("android")
  alias(libs.plugins.sgp.base)
}

slack {
  features {
    daggerRuntimeOnly()
  }
}

dependencies {
  api(libs.androidx.fragment)
}