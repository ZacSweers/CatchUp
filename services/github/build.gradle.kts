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

plugins {
  alias(libs.plugins.sgp.base)
  id("com.android.library")
  kotlin("android")
  alias(libs.plugins.apollo)
}

android {
  defaultConfig {
    buildConfigField("String", "GITHUB_DEVELOPER_TOKEN",
        "\"${properties["catchup_github_developer_token"]}\"")
  }
  buildFeatures {
    buildConfig = true
    androidResources = true
  }
  namespace = "io.sweers.catchup.service.github"
}

apollo {
  service("github") {
    customScalarsMapping.set(mapOf(
        "DateTime" to "kotlinx.datetime.Instant",
        "URI" to "okhttp3.HttpUrl"
    ))
    packageName.set("io.sweers.catchup.service.github")
    schemaFile.set(file("src/main/graphql/io/sweers/catchup/service/github/schema.json"))
  }
}

slack {
  features {
    dagger()
  }
}

dependencies {
  api(project(":service-api"))
  api(libs.apollo.runtime)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.okhttp.core)
  api(libs.retrofit.core)
  api(libs.retrofit.rxJava3)
  api(projects.libraries.appconfig)
  api(projects.libraries.di)

  implementation(project(":libraries:gemoji"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(libs.apollo.httpcache)
  // Apollo
  implementation(libs.apollo.runtime)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.jsoup)
  implementation(libs.misc.timber)
  implementation(libs.okhttp.core)

  compileOnly(libs.misc.javaxInject)
}
