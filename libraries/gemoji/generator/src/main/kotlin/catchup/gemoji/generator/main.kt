package catchup.gemoji.generator

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import catchup.gemoji.db.mutable.GemojiDatabase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter

private class GemojiGenerator : CliktCommand() {

  private val json by
    option("--json", help = "Path to the gemoji json file")
      .file(mustExist = true, canBeDir = false, mustBeReadable = true)
      .required()

  private val dbFile by
    option("--db", help = "Path to the gemoji database")
      .file(mustExist = false, canBeDir = false)
      .required()

  @OptIn(ExperimentalStdlibApi::class)
  private val gemojiJsonAdapter = Moshi.Builder().build().adapter<List<GemojiJson>>()

  override fun run() {
    if (dbFile.exists()) {
      dbFile.delete()
    }

    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    GemojiDatabase.Schema.create(driver)

    val db = GemojiDatabase(driver)
    populateDb(db)
    verifyDb(db)
  }

  private fun populateDb(db: GemojiDatabase) {
    val gemojiList = gemojiJsonAdapter.fromJson(json.readText())!!
    echo("Loaded ${gemojiList.size} gemojis from ${json.absolutePath}")

    var gemojiCount = 0
    var aliasCount = 0
    for ((emoji, aliases) in gemojiList) {
      if (aliases.isEmpty()) {
        echo("Skipping $emoji (empty aliases)")
        continue
      }
      gemojiCount++
      for (alias in aliases) {
        aliasCount++
        db.mutableGemojiQueries.insert(alias, emoji)
      }
    }
    echo("Inserted $aliasCount aliases for $gemojiCount gemojis")
  }

  // Load some common aliases to ensure it worked correctly
  private fun verifyDb(db: GemojiDatabase) {
    assertAlias(db, "smile", "😄")
    assertAlias(db, "laughing", "😆")
    assertAlias(db, "satisfied", "😆")
    assertAlias(db, "+1", "👍")
    assertAlias(db, "thumbsup", "👍")
    assertAlias(db, "gb", "🇬🇧")
    assertAlias(db, "uk", "🇬🇧")
  }

  private fun assertAlias(db: GemojiDatabase, alias: String, emoji: String) {
    val result = db.gemojiQueries.getEmoji(alias).executeAsOne().emoji
    check(result == emoji) { "Expected $alias to be $emoji, but was $result" }
  }
}

fun main(args: Array<String>) {
  GemojiGenerator().main(args)
}

@JsonClass(generateAdapter = true)
data class GemojiJson(val emoji: String, val aliases: List<String>)
