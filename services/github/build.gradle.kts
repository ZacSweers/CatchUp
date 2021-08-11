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
  id(deps.anvil.pluginId)
  id("com.apollographql.apollo3")
}

android {
  defaultConfig {
    buildConfigField("String", "GITHUB_DEVELOPER_TOKEN",
        "\"${properties["catchup_github_developer_token"]}\"")
  }
  buildFeatures {
    buildConfig = true
  }
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

dependencies {
  compileOnly(deps.misc.javaxInject)

  implementation(project(":libraries:gemoji"))
  implementation(project(":libraries:retrofitconverters"))
  implementation(project(":libraries:util"))
  implementation(deps.misc.jsoup)
  implementation(deps.retrofit.rxJava2)
  implementation(deps.okhttp.core)
  implementation(deps.kotlin.datetime)

  // Apollo
  implementation(deps.apollo.runtime)
  implementation(deps.apollo.rx2Support)

  api(project(":service-api"))
  api(deps.android.androidx.annotations)
  api(deps.dagger.runtime)
  api(deps.rx.java)
}
