#!/usr/bin/env kotlin
// Gradle plugin repo required for the module-graph-assert plugin dep
@file:Repository("https://plugins.gradle.org/m2")
@file:DependsOn("com.slack.foundry:cli:0.31.1")
@file:DependsOn("com.slack.foundry:skippy:0.31.0")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")
@file:DependsOn("com.fueledbycaffeine.spotlight:buildscript-utils:1.3.3")

import com.fueledbycaffeine.spotlight.buildscript.GradlePath
import com.fueledbycaffeine.spotlight.buildscript.SpotlightRulesList
import com.fueledbycaffeine.spotlight.buildscript.gradlePathRelativeTo
import com.fueledbycaffeine.spotlight.buildscript.graph.BreadthFirstSearch
import com.fueledbycaffeine.spotlight.buildscript.graph.DependencyRule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.jraska.module.graph.DependencyGraph
import foundry.cli.projectDirOption
import foundry.cli.skipBuildAndCacheDirs
import foundry.cli.walkEachFile
import java.io.ObjectOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeLines
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class DependencyGraphParser : CliktCommand() {
  override fun help(context: Context): String {
    return """
      A CLI for doing two things:
      1. Focus a specific target project and all its dependencies to generate a Spotlight-compatible ide-projects.txt file.
      2. Generate a serialized dependency graph of the whole repo for use with Skippy in pipeline uploads.
    """
      .trimIndent()
  }

  private val target by
    argument("--target", help = "Target project (in the form of :path:to:project)").optional()

  private val computeGraph by
    option("--compute-graph", help = "Generate a graph for the entire project").flag()

  private val verbose by option("--verbose", help = "Enables verbose logging").flag()
  private val printOnly by option("--print-only", help = "Prints only").flag()

  private val rootDir by projectDirOption()

  private val outputFile by
    option("--output-file", help = "Output file to write the graph to.")
      .path(mustExist = false, canBeFile = true)

  @OptIn(ExperimentalPathApi::class)
  override fun run() {
    check(computeGraph xor (target != null)) {
      "Exactly one of --compute-graph or a specified target argument may be specified!"
    }

    val (message, duration) =
      measureTimedValue {
        val allProjects =
          rootDir
            .toAbsolutePath()
            .walkEachFile { skipBuildAndCacheDirs() }
            .filter { it.name == "build.gradle.kts" }
            .map { it.parent.gradlePathRelativeTo(rootDir) }
            .toList()

        val rules = SpotlightRulesList(rootDir).read()

        // If focusing one target, find it and BFS it
        // If parsing whole repo, skip BFS and compute the whole graph
        target?.let { focusTarget(it, rules.implicitRules) }
          ?: computeGraph(rules.implicitRules, allProjects)
      }

    if (verbose) {
      echo(message(duration))
    }
  }

  private fun String.toGradlePath(): GradlePath {
    val buildFilePath =
      rootDir.resolve(removePrefix(":").replace(':', '/')).resolve("build.gradle.kts")
    check(buildFilePath.exists()) { "No expected build file for '$this' found at '$buildFilePath'" }

    return buildFilePath.parent.gradlePathRelativeTo(rootDir)
  }

  @OptIn(ExperimentalPathApi::class)
  private fun focusTarget(target: String, rules: Set<DependencyRule>): (Duration) -> String {
    val targetProject = target.toGradlePath()
    val requiredProjects = BreadthFirstSearch.flatten(setOf(targetProject), rules)

    val lines = requiredProjects.asSequence().map { it.path }.plus(targetProject.path).sorted()
    if (printOnly) {
      echo(lines.joinToString("\n"))
      echo("Spotlight found ${lines.count()} projects for $target")
    } else {
      val outputFile = outputFile ?: rootDir.resolve("gradle/ide-projects.txt")
      outputFile.writeLines(lines)
    }

    return {
      "Computing ${requiredProjects.size} entries in ide-projects.txt for root '$target' took $it"
    }
  }

  @OptIn(ExperimentalPathApi::class)
  private fun computeGraph(
    rules: Set<DependencyRule>,
    allProjects: List<GradlePath>,
  ): (Duration) -> String {
    val outputFile = outputFile ?: error("An output file is required to compute the whole graph!")
    val allEdges =
      allProjects
        .flatMap { gradlePath ->
          gradlePath.findSuccessors(rules).map { successor -> gradlePath.path to successor.path }
        }
        .toList()

    val graph = DependencyGraph.create(allEdges).serializableGraph()
    outputFile.deleteIfExists()
    outputFile.createParentDirectories()
    ObjectOutputStream(outputFile.outputStream()).use { it.writeObject(graph) }

    return { "Computing ${allEdges.size} dependencies took $it" }
  }
}

DependencyGraphParser().main(args)
