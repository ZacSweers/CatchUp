package io.sweers.catchup.skunkworks.benchmark.dataparse

import java.util.Locale

fun main() {

  val data = """
benchmark:     7,917,396 ns SpeedTest.moshi_kotlin_reflective_buffer_fromJson_minified
benchmark:     6,119,531 ns SpeedTest.gson_reflective_string_fromJson
benchmark:     5,267,761 ns SpeedTest.gson_autovalue_string_fromJson
benchmark:     5,675,990 ns SpeedTest.moshi_kotlin_codegen_buffer_fromJson_minified
benchmark:     5,681,614 ns SpeedTest.kserializer_string_fromJson_minified
benchmark:     7,589,376 ns SpeedTest.moshi_autovalue_buffer_fromJson
benchmark:     4,853,646 ns SpeedTest.gson_autovalue_string_fromJson_minified
benchmark:     5,421,251 ns SpeedTest.moshi_autovalue_string_toJson
benchmark:     8,762,918 ns SpeedTest.moshi_autovalue_string_fromJson
benchmark:     6,539,064 ns SpeedTest.moshi_kotlin_codegen_string_fromJson_minified
benchmark:   159,748,923 ns SpeedTest.gson_autovalue_buffer_toJson
benchmark:     7,617,241 ns SpeedTest.moshi_kotlin_codegen_buffer_fromJson
benchmark:     9,882,917 ns SpeedTest.moshi_kotlin_reflective_buffer_fromJson
benchmark:     4,029,636 ns SpeedTest.moshi_kotlin_codegen_buffer_toJson
benchmark:     8,892,449 ns SpeedTest.moshi_kotlin_codegen_string_fromJson
benchmark:    11,145,313 ns SpeedTest.moshi_kotlin_reflective_string_fromJson
benchmark:     7,427,344 ns SpeedTest.moshi_kotlin_reflective_buffer_toJson
benchmark:     7,548,490 ns SpeedTest.moshi_autovalue_buffer_fromJson_minified
benchmark:     6,401,875 ns SpeedTest.kserializer_string_fromJson
benchmark:     6,454,845 ns SpeedTest.gson_autovalue_string_toJson
benchmark:     6,475,990 ns SpeedTest.moshi_autovalue_string_fromJson_minified
benchmark:     6,152,761 ns SpeedTest.moshi_reflective_string_toJson
benchmark:     5,434,428 ns SpeedTest.moshi_kotlin_codegen_string_toJson
benchmark:     5,239,636 ns SpeedTest.kserializer_string_toJson
benchmark:     8,142,605 ns SpeedTest.gson_reflective_string_toJson
benchmark:     4,031,564 ns SpeedTest.moshi_autovalue_buffer_toJson
benchmark:    10,481,563 ns SpeedTest.moshi_reflective_string_fromJson
benchmark:     8,669,168 ns SpeedTest.moshi_kotlin_reflective_string_toJson
benchmark:     7,048,855 ns SpeedTest.gson_autovalue_buffer_fromJson
benchmark:     6,158,126 ns SpeedTest.gson_autovalue_buffer_fromJson_minified
  """.trimIndent()

  // Skip the header line
  val results = data.lineSequence()
      .map { line ->
        // benchmark:     6,154,949 ns SpeedTest.gson_autovalue_buffer_fromJson_minified
        val (_, score, units, benchmark) = line.split("\\s+".toRegex())
        Analysis(
            benchmark = benchmark,
            score = score.replace(",", "").toLong(),
            units = units
        )
      }
      .toList()

  ResultType.values().forEach { printResults(it, results) }
}

private fun printResults(type: ResultType, results: List<Analysis>) {
  val groupedResults = type.groupings.associate { grouping ->
    grouping to results.filter {
      grouping.matchFunction(it.benchmark)
    }
  }
  val benchmarkLength = results.maxBy { it.benchmark.length }!!.benchmark.length
  val scoreLength = results.maxBy { it.formattedScore.length }!!.formattedScore.length

  check(groupedResults.values.flatten().size == results.size) {
    "Missing information!"
  }

  val output = buildString {
    appendln()
    append(type.description)
    appendln(':')
    appendln()
    appendln("```")
    groupedResults.entries
        .joinTo(this, "\n\n", postfix = "\n```") { (grouping, matchedAnalyses) ->
          val content = matchedAnalyses.sortedBy { it.score }
              .joinToString("\n") { it.formattedString(benchmarkLength, scoreLength) }
          "${grouping.name}\n$content"
        }
  }

  println(output)
}

private enum class ResultType(val description: String, val groupings: List<Grouping>) {
  SERIALIZATION_TYPE(
      description = "Grouped by serialization type (read, write, buffered, string)",
      groupings = listOf(
          Grouping("Read (buffered)") {
            "_buffer" in it && "_fromJson" in it
          },
          Grouping("Read (string)") {
            "_string" in it && "_fromJson" in it
          },
          Grouping("Write (buffered)") {
            "_buffer" in it && "_toJson" in it
          },
          Grouping("Write (string)") {
            "_string" in it && "_toJson" in it
          }
      )
  ),
  LIBRARY(
      description = "Grouped by library (interesting to see how reflection vs custom adapters affects perf within a library)",
      groupings = listOf(
          Grouping("GSON") {
            "gson" in it
          },
          Grouping("Kotlinx Serialization") {
            "kserializer" in it
          },
          Grouping("Moshi") {
            "moshi_" in it && "kotlin" !in it
          },
          Grouping("Moshi Kotlin") {
            "moshi_kotlin" in it
          }
      )
  )
}

private data class Grouping(
    val name: String,
    val matchFunction: (String) -> Boolean
)

private data class Analysis(
    val benchmark: String,
    val score: Long,
    val units: String
) {
  override fun toString() = "$benchmark\t$score\t$units"

  fun formattedString(benchmarkLength: Int, scoreLength: Int): String {
    return String.format(Locale.US,
        "%-${benchmarkLength}s  %${scoreLength}s  %s",
        benchmark, formattedScore, units)
  }

  val formattedScore: String
    get() = String.format(Locale.US, "%,d", score)
}

private operator fun <T> List<T>.component6(): T = this[5]
private operator fun <T> List<T>.component7(): T = this[6]
