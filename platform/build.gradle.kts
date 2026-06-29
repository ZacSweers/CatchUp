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
import foundry.gradle.Platforms

plugins {
  alias(libs.plugins.foundry.base)
  `java-platform`
}

val catalogExtension =
  extensions.findByType<VersionCatalogsExtension>() ?: error("Could not find any version catalogs!")

for (name in catalogExtension.catalogNames) {
  val catalog = catalogExtension.named(name)
  Platforms.applyFromCatalog(project, catalog)
}
