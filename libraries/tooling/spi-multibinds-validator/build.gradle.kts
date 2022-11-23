plugins {
  kotlin("jvm")
  kotlin("kapt")
  alias(libs.plugins.sgp.base)
}

dependencies {
  kapt(libs.auto.service)
  compileOnly(libs.auto.service)

  implementation(libs.dagger.runtime)
  implementation(libs.dagger.spi)
}
