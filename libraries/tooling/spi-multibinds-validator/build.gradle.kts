plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.ksp)
}

dependencies {
  ksp(libs.autoService.ksp)

  implementation(libs.dagger.spi)
  implementation(libs.misc.debug.guava)

  compileOnly(libs.autoService.annotations)
}
