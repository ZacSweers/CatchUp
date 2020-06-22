/*
 * Copyright (c) 2020 Zac Sweers
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
  `java-library`
  id("tick-tock")
}

tickTock {
  tzVersion.set(providers.gradleProperty("tzdbVersion")
      .forUseAtConfigurationTime())
  // Note: we generate and check these in rather than count on gradle caching
  tzOutputDir.set(layout.projectDirectory.dir("src/main/resources"))
  codeOutputDir.set(layout.projectDirectory.dir("src/main/java"))
}

dependencies {
  compileOnly(deps.android.androidx.annotations)
  api(deps.kotlin.coroutines)
  api(deps.kotlin.stdlib.core)

  testImplementation(deps.test.junit)
  testImplementation(deps.test.truth)
}
