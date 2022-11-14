plugins {
  `java-library`
  alias(libs.plugins.sgp.base)
}

dependencies {
  annotationProcessor(libs.auto.service)
  compileOnly(libs.auto.service)

  implementation(libs.dagger.runtime)
  implementation(libs.dagger.spi)
  implementation(libs.javapoet)
}
