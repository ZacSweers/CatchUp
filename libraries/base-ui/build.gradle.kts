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
  libraryVariants.all {
    generateBuildConfigProvider?.configure {
      enabled = false
    }
  }
}

kapt {
  correctErrorTypes = true
  mapDiagnosticLocations = true

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
  api(project(":libraries:util"))
  api(deps.dagger.android.runtime)
  api(deps.dagger.android.support)
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.jdk7)
  api(deps.autoDispose.core)
  api(deps.autoDispose.android)
  api(deps.autoDispose.lifecycle)
  api(deps.autoDispose.androidArch)
  api(deps.rx.java)
  implementation(deps.rx.relay)
  api(deps.android.androidx.annotations)
  api(deps.android.androidx.appCompat)
  api(deps.android.androidx.core)
  api(deps.android.androidx.design)
  api(deps.android.androidx.palette)
  api(deps.android.androidx.paletteKtx)
}
