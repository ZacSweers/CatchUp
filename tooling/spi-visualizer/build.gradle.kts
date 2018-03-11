import org.gradle.api.JavaVersion.VERSION_1_8

plugins {
//  kotlin("jvm")
//  kotlin("kapt")
  id("java-library")
}

//apply {
//  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
//}

//kapt {
//  correctErrorTypes = true
//  useBuildCache = true
//  mapDiagnosticLocations = true
//}

java {
  sourceCompatibility = VERSION_1_8
  targetCompatibility = VERSION_1_8
}

dependencies {
  annotationProcessor(deps.auto.service)
  compileOnly(deps.auto.service)

  implementation(deps.dagger.runtime)
  implementation(deps.dagger.spi)
  implementation(deps.build.javapoet)
}
