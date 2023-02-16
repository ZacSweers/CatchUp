plugins {
  `java-library`
  alias(libs.plugins.sgp.base)
}

dependencies {
  implementation(libs.dagger.runtime)
  implementation(libs.dagger.spi)
  implementation(libs.javapoet)

  compileOnly(libs.auto.service)

  annotationProcessor(libs.auto.service)
}
