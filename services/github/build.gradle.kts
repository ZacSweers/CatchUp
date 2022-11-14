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
  id("com.android.library")
  kotlin("android")
  alias(libs.plugins.apollo)
  alias(libs.plugins.sgp.base)
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
    @Suppress("UnstableApiUsage")
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
  compileOnly(libs.misc.javaxInject)

  implementation(project(":libraries:gemoji"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(libs.misc.jsoup)
  implementation(libs.retrofit.rxJava3)
  implementation(libs.okhttp.core)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlin.coroutinesRx)

  // Apollo
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.httpcache)

  api(project(":service-api"))
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.rx.java)
}
