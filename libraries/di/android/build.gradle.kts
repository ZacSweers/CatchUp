plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.foundry.base)
}

foundry {
  features {
    daggerRuntimeOnly()
  }
}
