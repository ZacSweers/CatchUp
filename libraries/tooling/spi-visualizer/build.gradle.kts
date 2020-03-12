plugins {
  `java-library`
}

dependencies {
  annotationProcessor(deps.auto.service)
  compileOnly(deps.auto.service)

  implementation(deps.dagger.spi)
  implementation(deps.build.javapoet)
}
