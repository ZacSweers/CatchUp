/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.foundry.base)
  application
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.moshix)
}

application { mainClass.set("catchup.gemoji.generator.MainKt") }

sqldelight {
  databases {
    create("GemojiDatabase") {
      // Must be set before dependencies
      packageName.set("catchup.gemoji.db.mutable")
      dependency(project(":libraries:gemoji:db"))
      schemaOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/databases"))
      migrationOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/migrations"))
    }
  }
}

dependencies {
  implementation(project(":libraries:gemoji:db"))
  implementation(libs.clikt)
  implementation(libs.sqldelight.driver.jvm)
}
