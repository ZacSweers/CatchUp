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
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
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
  namespace = "catchup.service.producthunt"
}

apollo {
  // https://api.producthunt.com/v2/api/graphql
  service("producthunt") {
    mapScalar("DateTime", "kotlinx.datetime.Instant")
    packageName.set("catchup.service.producthunt")
    schemaFiles.from(file("src/main/graphql/catchup/service/producthunt/schema.graphqls"))
  }
}

foundry {
  features {
    dagger()
    moshi(codegen = false)
  }
  android {
    features {
      resources("catchup_service_ph_")
    }
  }
}

dependencies {
  api(libs.apollo.api)
  api(libs.apollo.runtime)
  api(libs.dagger.runtime)
  api(libs.kotlin.datetime)
  api(libs.moshi.core)
  api(libs.okhttp.core)
  api(projects.libraries.auth)
  api(projects.libraries.di)
  api(projects.libraries.util)
  api(projects.serviceApi)

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.apollo.httpcache)
  implementation(libs.kotlin.datetime)
  implementation(libs.misc.okio)
  implementation(libs.okhttp.core)
}
