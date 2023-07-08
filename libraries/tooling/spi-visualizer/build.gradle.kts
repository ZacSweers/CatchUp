plugins {
  `java-library`
  alias(libs.plugins.sgp.base)
}

dependencies {
  implementation(libs.dagger.spi)
  implementation(libs.javapoet)
  implementation(libs.misc.debug.guava)

  compileOnly(libs.auto.service)

  annotationProcessor(libs.auto.service)
}
