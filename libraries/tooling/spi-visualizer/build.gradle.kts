plugins {
  `java-library`
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.ksp)
}

dependencies {
  ksp(libs.autoService.ksp)

  implementation(libs.dagger.spi)
  implementation(libs.errorProneAnnotations)
  implementation(libs.javapoet)
  implementation(libs.misc.debug.guava)

  compileOnly(libs.autoService.annotations)
}
