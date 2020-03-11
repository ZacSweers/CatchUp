import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
    jcenter()
  }
}

repositories {
  mavenCentral()
  google()
  jcenter()
}

plugins {
  kotlin("jvm") version "1.3.70"
  `kotlin-dsl`
  `java-gradle-plugin`
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

gradlePlugin {
  plugins {
    create("CatchUpPlugin") {
      id = "catchup"
      implementationClass = "dev.zacsweers.catchup.gradle.CatchUpPlugin"
    }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += listOf("-Xuse-experimental=kotlin.ExperimentalStdlibApi")
  }
}

dependencies {
  implementation("com.android.tools.build:gradle:4.1.0-alpha02")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.70")
}
