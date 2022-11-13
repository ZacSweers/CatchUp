import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
  mavenCentral()
  google()
  maven("https://storage.googleapis.com/r8-releases/raw")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
}

plugins {
  kotlin("jvm") version "1.7.0"
  `kotlin-dsl`
  `java-gradle-plugin`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

/**
 * These are magic shared versions that are used in both buildSrc's build file and dependencies.kt.
 * These are copied as a source into the main source set and templated for replacement.
 */
object SharedBuildVersions {
  const val agp = "8.0.0-alpha08"
  const val kotlin = "1.7.21"
  const val moshi = "1.14.0"
  const val okio = "3.2.0"
  const val kotlinJvmTarget = "11"
  val kotlinCompilerArgs = listOf(
      "-progressive",
      "-Xjsr305=strict",
      "-opt-in=kotlin.contracts.ExperimentalContracts",
      "-opt-in=kotlin.experimental.ExperimentalTypeInference",
      "-opt-in=kotlin.ExperimentalStdlibApi",
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=kotlin.time.ExperimentalTime",
//      "-opt-in=kotlinx.coroutines.FlowPreview",
//      "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      // Potentially useful for static analysis tools or annotation processors.
      "-Xemit-jvm-type-annotations",
      // Match JVM assertion behavior: https://publicobject.com/2019/11/18/kotlins-assert-is-not-like-javas-assert/
      "-Xassertions=jvm",
      "-Xproper-ieee754-comparisons",
      // Enable new jvmdefault behavior
      // https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/
      "-Xjvm-default=all"
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
    freeCompilerArgs += SharedBuildVersions.kotlinCompilerArgs - "-progressive"
    jvmTarget = SharedBuildVersions.kotlinJvmTarget
  }
}

dependencies {
  // Explicitly declare all the kotlin bits to avoid mismatched versions
  implementation(kotlin("gradle-plugin", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-common", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-jdk7", version = SharedBuildVersions.kotlin))
  implementation(kotlin("stdlib-jdk8", version = SharedBuildVersions.kotlin))
  implementation(kotlin("reflect", version = SharedBuildVersions.kotlin))

  compileOnly("com.google.devtools.ksp:symbol-processing-gradle-plugin:1.7.21-1.0.8")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
  implementation("com.android.tools.build:gradle:${SharedBuildVersions.agp}")
  implementation("com.squareup.moshi:moshi:${SharedBuildVersions.moshi}")
  implementation("com.squareup.okio:okio:${SharedBuildVersions.okio}")
  implementation("de.undercouch:gradle-download-task:4.1.1")
  implementation("com.squareup:javapoet:1.13.0")
  implementation("com.squareup.anvil:gradle-plugin:2.4.2")
}
