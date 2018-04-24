import org.gradle.api.JavaVersion.VERSION_1_8

plugins {
  kotlin("jvm")
}

dependencies {
  compile(deps.crumb.annotations)
}
