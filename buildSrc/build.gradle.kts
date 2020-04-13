import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
    maven {
      setUrl("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    jcenter()
  }
}

repositories {
  mavenCentral()
  google()
  maven {
    setUrl("https://dl.bintray.com/kotlin/kotlin-eap")
  }
  jcenter()
}

plugins {
  kotlin("jvm") version "1.3.71"
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
    create("LicensesJsonGeneratorPlugin") {
      id = "licensesJsonGenerator"
      implementationClass = "dev.zacsweers.catchup.gradle.LicensesJsonGeneratorPlugin"
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
  implementation("com.android.tools.build:gradle:4.1.0-alpha05")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71")
  implementation("com.squareup.moshi:moshi:1.9.2")
  implementation("com.squareup.okio:okio:2.5.0")
}
