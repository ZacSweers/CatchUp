/*
 * Copyright (c) 2018 Zac Sweers
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

apply {
  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)

  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
    vectorDrawables.useSupportLibrary = true
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  lintOptions {
    isCheckReleaseBuilds = false
    isAbortOnError = false
  }
  libraryVariants.all {
    generateBuildConfigProvider?.configure {
      enabled = false
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = build.standardFreeKotlinCompilerArgs
    jvmTarget = "1.8"
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
  kapt(project(":service-registry:service-registry-compiler"))
  kapt(deps.crumb.compiler)
  kapt(deps.dagger.apt.compiler)

  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(deps.misc.jsoup)
  implementation(deps.retrofit.core)
  implementation(deps.retrofit.rxJava2)
  implementation(deps.okhttp.core)

  api(project(":service-api"))
  api(deps.android.androidx.annotations)
  api(deps.dagger.runtime)
  api(deps.misc.lazythreeten)
  api(deps.rx.java)
}
