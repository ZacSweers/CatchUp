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
@file:DependsOn("com.slack.foundry:cli:0.35.2")
@file:OptIn(ExperimentalPathApi::class)

import foundry.cli.skipBuildAndCacheDirs
import foundry.cli.walkEachFile
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Suppress("DEPRECATION")
fun camel(s: String) = s.split('-').joinToString("") { it.capitalize() }.decapitalize()

fun accessor(path: String): String =
  path.removePrefix(":").split(":").joinToString(".") { camel(it) }.let { "projects.$it" }

val rootDir = Paths.get(args.firstOrNull() ?: ".").toAbsolutePath().normalize()

val projectsFile = rootDir.resolve("gradle/all-projects.txt")

require(projectsFile.exists()) { "gradle/all-projects.txt not found" }

val map =
  projectsFile.readLines().filter { it.isNotBlank() }.associateBy(::accessor) // accessor -> path

println("Will rewrite ${map.size} project accessors.")

// Build longest-first list so "projects.di.core" is replaced before "projects.di"
val accessors = map.keys.sortedByDescending { it.length }

rootDir
  .absolute()
  .walkEachFile { skipBuildAndCacheDirs() }
  .filter { it.name == "build.gradle.kts" }
  .forEach { file ->
    var txt = file.readText()

    for (acc in accessors) {
      val path = map.getValue(acc)
      val esc = Regex.escape(acc) // projects.foo.bar → projects\.foo\.bar

      // project(projects.foo.path) → project(":foo")
      txt = txt.replace(Regex("""project\(\s*$esc\.path\s*\)"""), """project("$path")""")

      // bare accessor → project(":foo")
      //    • \b before acc
      //    • (?!\.) negative look-ahead stops at a dot, preventing partial hit
      txt = txt.replace(Regex("""\b$esc\b(?!\.)"""), """project("$path")""")
    }

    file.writeText(txt)
    println("updated ${file.absolutePathString()}")
  }
