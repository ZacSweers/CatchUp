plugins {
  `java-library`
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  annotationProcessor(deps.auto.service)
  compileOnly(deps.auto.service)

  implementation(deps.dagger.runtime)
  implementation(deps.dagger.spi)
  implementation(deps.build.javapoet)
}
