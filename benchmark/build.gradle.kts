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

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
  id("androidx.benchmark")
  id("kotlinx-serialization")
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)
  buildToolsVersion(deps.android.build.buildToolsVersion)

  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
    setTestInstrumentationRunner("androidx.benchmark.AndroidBenchmarkRunner")
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
    findByName("androidTest")?.java?.srcDirs("src/androidTest/kotlin")
  }
  libraryVariants.all {
    generateBuildConfigProvider?.configure {
      enabled = false
    }
  }
}

dependencies {
  kapt("com.google.auto.value:auto-value-annotations:1.6.5")
  kapt("com.google.auto.value:auto-value:1.6.5")
  kapt("com.ryanharter.auto.value:auto-value-gson:1.0.0")
  kapt("com.ryanharter.auto.value:auto-value-moshi:0.4.5")
  kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
  compileOnly("com.google.auto.value:auto-value-annotations:1.6.5")
  compileOnly("com.ryanharter.auto.value:auto-value-moshi-annotations:0.4.5")
  androidTestImplementation("com.google.guava:guava:27.1-jre")
  androidTestImplementation("com.squareup.okio:okio:2.2.2")
  androidTestImplementation("androidx.benchmark:benchmark:1.0.0-alpha01")
  androidTestImplementation("junit:junit:4.12")
  androidTestImplementation("androidx.test:runner:1.1.1")
  androidTestImplementation("androidx.test:rules:1.1.1")
  androidTestImplementation("androidx.test.ext:junit:1.1.0")
  implementation("com.ryanharter.auto.value:auto-value-gson-runtime:1.0.0")
  implementation("com.google.auto.value:auto-value-annotations:1.6.5")
  implementation("com.google.guava:guava:27.1-jre")
  implementation("com.squareup.moshi:moshi-kotlin:1.8.0")
  implementation("com.squareup.okio:okio:2.2.2")
  implementation("com.squareup.moshi:moshi:1.8.0")
  implementation("com.google.code.gson:gson:2.8.5")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.31")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.0")
  androidTestImplementation(configurations.getByName("implementation"))
}
