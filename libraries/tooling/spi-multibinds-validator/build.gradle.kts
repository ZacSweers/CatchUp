plugins {
  kotlin("jvm")
  kotlin("kapt")
  alias(libs.plugins.sgp.base)
}

dependencies {
  implementation(libs.dagger.spi)
  implementation(libs.misc.debug.guava)

  compileOnly(libs.auto.service)

  kapt(libs.auto.service)
}
