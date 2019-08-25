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

dependencies {
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.jdk7)
  api(deps.corbind.core)
  api(deps.corbind.androidx.core)
  api(deps.corbind.material)

  api(deps.android.androidx.annotations)
  api(deps.android.androidx.appCompat)
  api(deps.android.androidx.core)
  api(deps.android.androidx.design)
}
