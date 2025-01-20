#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.2")
@file:DependsOn("com.squareup.moshi:moshi:1.15.2")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.15.2")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readLines
import okio.buffer
import okio.sink
import okio.source

@Suppress("UNCHECKED_CAST")
class SarifCleaner : CliktCommand() {

  private val sarifFile by option("--sarif-file").path().required()
  private val modifiedFiles by option("--modified-files").path().required()
  private val outputFile by option("--output-file").path().required()

  override fun help(context: Context) =
    "Filters a sarif file to only include results for modified files"

  override fun run() {
    if (!sarifFile.exists()) {
      // Nothing to do, probably a build failure
      return
    }
    outputFile.deleteIfExists()
    outputFile.createParentDirectories()

    echo("Parsing sarif file: $sarifFile")
    val modifiedFiles = modifiedFiles.readLines().toSet()
    val jsonValue =
      JsonReader.of(sarifFile.source().buffer()).readJsonValue() as MutableMap<String, Any?>
    // /runs/0/results/0/locations/0/physicalLocation/artifactLocation/uri
    val runs = jsonValue["runs"] as MutableList<Map<String, Any?>>
    val results = runs[0]["results"] as MutableList<Map<String, Any?>>
    val filteredResults =
      results.filter { result ->
        val locations = result["locations"] as List<Map<String, Any?>>
        locations.any { location ->
          val physicalLocation = location["physicalLocation"] as Map<String, Any?>
          val artifactLocation = physicalLocation["artifactLocation"] as Map<String, Any?>
          val uri = artifactLocation["uri"] as String
          uri in modifiedFiles
        }
      }

    echo("Found ${results.size} result(s), filtered to ${filteredResults.size} result(s)")

    // Now put the filtered results back into the original json
    jsonValue["runs"] = listOf(runs[0].toMutableMap().apply { this["results"] = filteredResults })

    // Finally, change all .0 doubles to Ints because that's what the sarif spec says
    fixInts(jsonValue)

    outputFile.createFile()
    echo("Writing ${filteredResults.size} result(s) to $outputFile")
    JsonWriter.of(outputFile.sink().buffer()).use { writer ->
      writer.indent = "  "
      writer.jsonValue(jsonValue)
    }
  }

  private fun fixInts(map: MutableMap<String, Any?>) {
    for ((key, value) in map) {
      when (value) {
        is Double -> {
          if (value.isWholeNumber) {
            map[key] = value.toInt()
          }
        }

        is Map<*, *> -> {
          fixInts(value as MutableMap<String, Any?>)
        }

        is List<*> -> {
          fixInts(value as MutableList<Any?>)
        }
      }
    }
  }

  private fun fixInts(list: MutableList<Any?>) {
    for ((i, item) in list.toList().withIndex()) {
      when (item) {
        is Map<*, *> -> {
          fixInts(item as MutableMap<String, Any?>)
        }

        is Double -> {
          if (item.isWholeNumber) {
            list[i] = item.toInt()
          }
        }
      }
    }
  }

  private val Double.isWholeNumber
    get() = this % 1 == 0.0
}

SarifCleaner().main(args)
