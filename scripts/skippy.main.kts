#!/usr/bin/env kotlin
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
// Gradle plugin repo required for the module-graph-assert plugin dep
@file:Repository("https://plugins.gradle.org/m2")
@file:DependsOn("com.slack.foundry:cli:0.35.2")
@file:DependsOn("com.slack.foundry:skippy:0.35.2")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.1.0")

import com.github.ajalt.clikt.command.main
import foundry.skippy.ComputeAffectedProjectsCli
import kotlinx.coroutines.runBlocking

runBlocking { ComputeAffectedProjectsCli().main(args) }
