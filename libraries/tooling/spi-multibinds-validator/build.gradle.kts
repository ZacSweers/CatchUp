plugins {
  kotlin("jvm")
  kotlin("kapt")
  alias(libs.plugins.sgp.base)
}

dependencies {
  implementation(libs.misc.debug.guava)
  implementation(libs.dagger.spi)

  compileOnly(libs.auto.service)

  kapt(libs.auto.service)
}
