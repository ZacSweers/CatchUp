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
    buildConfigField("String", "PRODUCT_HUNT_CLIENT_ID",
        "\"${project.properties["catchup_product_hunt_client_id"]}\"")
    buildConfigField("String", "PRODUCT_HUNT_CLIENT_SECRET",
        "\"${project.properties["catchup_product_hunt_client_secret"]}\"")
  }
  buildFeatures {
    buildConfig = true
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
  android {
    features {
      resources("catchup_service_ph_")
    }
  }
}

dependencies {
  api(libs.apollo.runtime)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.moshi.core)
  api(libs.okhttp.core)
  api(projects.libraries.auth)
  api(projects.libraries.di)
  api(projects.serviceApi)

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.apollo.httpcache)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.okio)
  implementation(libs.okhttp.core)
  implementation(projects.libraries.util)
}
