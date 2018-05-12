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

/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
}

apply {
  from(rootProject.file("gradle/config-kotlin-sources.gradle"))
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)
  buildToolsVersion(deps.android.build.buildToolsVersion)

  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
    vectorDrawables.useSupportLibrary = true
  }
  compileOptions {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
  }
  lintOptions {
    setLintConfig(file("lint.xml"))
    isAbortOnError = true
    check("InlinedApi")
    check("NewApi")
    fatal("NewApi")
    fatal("InlinedApi")
    enable("UnusedResources")
    isCheckReleaseBuilds = true
    textReport = deps.build.ci
    textOutput("stdout")
    htmlReport = !deps.build.ci
    xmlReport = !deps.build.ci
  }
}

kapt {
  correctErrorTypes = true
  useBuildCache = true
  mapDiagnosticLocations = true
  arguments {
    arg("moshi.generated", "javax.annotation.Generated")
  }
}

dependencies {
  kapt(project(":service-registry:service-registry-compiler"))
  kapt(deps.crumb.compiler)
  kapt(deps.dagger.apt.compiler)
  kapt(deps.moshi.compiler)

  implementation(project(":util"))
  implementation(deps.misc.okio)
  implementation(deps.moshi.core)
  implementation(deps.retrofit.core)
  implementation(deps.retrofit.moshi)
  implementation(deps.retrofit.rxJava2)
  implementation(deps.okhttp.core)
  implementation(deps.misc.moshiLazyAdapters)

  // Inspector
  compileOnly(deps.inspector.factoryCompiler.compileOnly.annotations)
  compileOnly(deps.inspector.apt.extensions.android)
  compileOnly(deps.inspector.apt.extensions.nullability)
  kapt(deps.inspector.apt.extensions.autovalue)
  kapt(deps.inspector.apt.compiler)
  kapt(deps.inspector.factoryCompiler.apt)
  implementation(deps.inspector.core)

  api(project(":service-api"))
  api(deps.android.support.annotations)
  api(deps.dagger.runtime)
  api(deps.misc.lazythreeten)
  api(deps.rx.java)
}
