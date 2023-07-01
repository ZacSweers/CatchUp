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
    buildConfigField("String", "PRODUCT_HUNT_DEVELOPER_TOKEN",
        "\"${project.properties["catchup_product_hunt_developer_token"]}\"")
  }
  buildFeatures {
    buildConfig = true
    androidResources = true
  }
  namespace = "io.sweers.catchup.service.producthunt"
}

apollo {
  // https://api.producthunt.com/v2/api/graphql
  service("producthunt") {
    customScalarsMapping.set(mapOf(
      "DateTime" to "kotlinx.datetime.Instant",
//      "URI" to "okhttp3.HttpUrl"
    ))
    packageName.set("io.sweers.catchup.service.producthunt")
    schemaFile.set(file("src/main/graphql/io/sweers/catchup/service/producthunt/schema.graphqls"))
  }
}

slack {
  features {
    dagger()
    moshi(codegen = true)
  }
}

dependencies {
  api(project(":service-api"))
  api(libs.androidx.annotations)
  api(libs.dagger.runtime)
  api(libs.rx.java)

  implementation(project(":libraries:util"))
  implementation(libs.apollo.httpcache)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.moshiLazyAdapters)
  implementation(libs.misc.okio)
  implementation(libs.moshi.core)
  implementation(libs.okhttp.core)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.moshi)
  implementation(libs.retrofit.rxJava3)
}
