/*
 * Copyright (c) 2019 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = build.standardFreeKotlinCompilerArgs
    jvmTarget = "1.8"
  }
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)

  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
    buildConfigField("String", "SMMRY_API_KEY",
        "\"${properties["catchup_smmry_api_key"]}\"")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  sourceSets {
    findByName("main")?.java?.srcDirs("src/main/kotlin")
    findByName("debug")?.java?.srcDirs("src/debug/kotlin")
    findByName("release")?.java?.srcDirs("src/release/kotlin")
    findByName("test")?.java?.srcDirs("src/test/kotlin")
  }
}

kapt {
  correctErrorTypes = true
  mapDiagnosticLocations = true
  arguments {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("moshi.generated", "javax.annotation.Generated")
  }

  // Compiling with JDK 11+, but kapt doesn't forward source/target versions.
  javacOptions {
    option("-source", "8")
    option("-target", "8")
  }
}

dependencies {
  kapt(deps.dagger.apt.compiler)
  kapt(deps.dagger.android.apt.processor)
  compileOnly(deps.misc.javaxInject)
  implementation(deps.dagger.runtime)
  implementation(deps.dagger.android.runtime)
  implementation(deps.dagger.android.support)
  implementation(deps.retrofit.core)
  implementation(deps.retrofit.moshi)
  implementation(project(":libraries:base-ui"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(deps.misc.lottie)
  kapt(deps.moshi.compiler)
  implementation(deps.moshi.core)
  implementation(deps.android.androidx.room.runtime)
  implementation(deps.android.androidx.room.ktx)
  kapt(deps.android.androidx.room.apt)
  implementation(deps.android.androidx.lifecycle.ktx)
  implementation(deps.kotlin.coroutines)
  implementation(deps.kotlin.stdlib.jdk7)

  api(project(":service-api"))
  api(deps.android.androidx.annotations)
  api(deps.android.androidx.appCompat)
  api(deps.android.androidx.core)
  api(deps.android.androidx.design)
}
