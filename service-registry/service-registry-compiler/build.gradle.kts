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

import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("kapt")
}

apply {
  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs = listOf("-Xjsr305=strict")
  }
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  mapDiagnosticLocations = true
}

dependencies {
  kapt(deps.auto.service)
  compileOnly(deps.auto.service)

  compile(project(":service-registry:service-registry-annotations"))
  compile(deps.auto.common)
  compile(deps.crumb.annotations)
  compile(deps.crumb.compilerApi)
  compile(deps.dagger.runtime)
  compile(deps.kotlin.metadata)
  compile(deps.kotlin.poet)
  compile(deps.kotlin.stdlib.jdk8)
}
