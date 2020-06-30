import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
  mavenCentral()
  google()
  maven("https://dl.bintray.com/kotlin/kotlin-eap")
  maven("https://storage.googleapis.com/r8-releases/raw")
  jcenter()
}

plugins {
  kotlin("jvm") version "1.4-M2"
  `kotlin-dsl`
  `java-gradle-plugin`
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

/**
 * These are magic shared versions that are used in both buildSrc's build file and dependencies.kt.
 * These are copied as a source into the main source set and templated for replacement.
 */
object SharedBuildVersions {
  const val agp = "4.2.0-alpha03"
  const val kotlin = "1.4-M2"
  const val moshi = "1.9.3"
  const val okio = "2.6.0"
  const val kotlinJvmTarget = "1.8"
  val kotlinCompilerArgs = listOf(
      "-progressive",
      "-Xinline-classes",
      "-Xjsr305=strict",
      "-Xjvm-default=enable",
      "-Xassertions=jvm",
      "-Xopt-in=kotlin.contracts.ExperimentalContracts",
      "-Xopt-in=kotlin.experimental.ExperimentalTypeInference",
      "-Xopt-in=kotlin.ExperimentalStdlibApi",
      "-Xopt-in=kotlin.RequiresOptIn",
      "-Xopt-in=kotlin.time.ExperimentalTime",
      "-Xskip-metadata-version-check",
      "-Xopt-in=kotlinx.coroutines.FlowPreview",
      "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
  )

  fun asTemplatesMap(): Map<String, String> {
    return mapOf(
        "agpVersion" to agp,
        "kotlinVersion" to kotlin,
        "moshiVersion" to moshi,
        "okioVersion" to okio,
        "kotlinCompilerArgs" to kotlinCompilerArgs.joinToString(", ") { "\"$it\"" },
        "kotlinJvmTarget" to kotlinJvmTarget
    )
  }
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

sourceSets {
  main.configure {
    java.srcDir(project.file("$buildDir/generated/sources/version-templates/kotlin/main"))
  }
}

val copyVersionTemplatesProvider = tasks.register<Copy>("copyVersionTemplates") {
  val templatesMap = SharedBuildVersions.asTemplatesMap()
  inputs.property("buildversions", templatesMap.hashCode())
  from(layout.projectDirectory.dir("version-templates"))
  into(project.layout.buildDirectory.dir("generated/sources/version-templates/kotlin/main"))
  expand(templatesMap)
  filteringCharset = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
  dependsOn(copyVersionTemplatesProvider)
  kotlinOptions {
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += SharedBuildVersions.kotlinCompilerArgs
    jvmTarget = SharedBuildVersions.kotlinJvmTarget
    languageVersion = "1.4"
  }
}

dependencies {
  implementation("com.android.tools:r8:2.1.44")

  // Explicitly declare all the kotlin bits to avoid mismatched versions
  implementation(kotlin("gradle-plugin", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-common", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-jdk7", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-jdk8", version = SharedBuildVersions.kotlin))
  implementation(kotlin("reflect", version = SharedBuildVersions.kotlin))

  implementation("com.android.tools.build:gradle:${SharedBuildVersions.agp}")
  implementation("com.squareup.moshi:moshi:${SharedBuildVersions.moshi}")
  implementation("com.squareup.okio:okio:${SharedBuildVersions.okio}")
  implementation("de.undercouch:gradle-download-task:4.0.4")
}
