plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.sgp.base)
}

slack {
  features {
    daggerRuntimeOnly()
  }
}
