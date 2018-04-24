import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("kapt")
}

apply {
  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = listOf("-Xjsr305=strict")
  }
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  mapDiagnosticLocations = true
}

dependencies {
  kapt(deps.auto.service)
  compileOnly(deps.auto.service)

  compile(project(":service-registry:service-registry-annotations"))
  compile(deps.auto.common)
  compile(deps.crumb.annotations)
  compile(deps.crumb.compilerApi)
  compile(deps.dagger.runtime)
  compile(deps.kotlin.metadata)
  compile(deps.kotlin.poet)
  compile(deps.kotlin.stdlib.jdk8)
}
