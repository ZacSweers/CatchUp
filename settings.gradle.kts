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
  id("com.gradle.enterprise") version "3.0"
}

include(
    ":app",
    ":libraries:base-ui",
    ":libraries:appconfig",
    ":libraries:gemoji",
    ":libraries:flowbinding",
    ":libraries:kotlinutil",
    ":libraries:retrofitconverters",
    ":libraries:smmry",
    ":libraries:tooling:spi-visualizer",
    ":libraries:util",
    ":service-api",
    ":service-registry:service-registry",
    ":service-registry:service-registry-annotations",
    ":service-registry:service-registry-compiler",
    ":services:designernews",
    ":services:dribbble",
    ":services:github",
    ":services:hackernews",
//    ":services:imgur",
    ":services:medium",
//    ":services:newsapi",
    ":services:producthunt",
    ":services:reddit",
    ":services:slashdot",
    ":services:unsplash",
    ":services:uplabs"
)
