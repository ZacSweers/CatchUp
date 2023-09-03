/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package catchup.gemoji

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.gemoji.db.GemojiDatabase
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/** Converts a markdown emoji alias (eg, ":smile:") into an android render-able emoji. */
interface EmojiMarkdownConverter {
  suspend fun convert(alias: String): String?
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class GemojiEmojiMarkdownConverter @Inject constructor(private val gemojiDatabase: GemojiDatabase) :
  EmojiMarkdownConverter {
  override suspend fun convert(alias: String): String? {
    println("Converting $alias")
    return withContext(Dispatchers.IO) {
      gemojiDatabase.gemojiQueries
        .getEmoji(alias)
        .asFlow()
        .mapToOne(Dispatchers.IO) // TODO double?
        .first()
        .emoji
        .also { println("Converted $alias to $it") }
    }
  }
}

/**
 * Returns a [String] that replaces occurrences of markdown emojis with android render-able emojis.
 */
suspend fun EmojiMarkdownConverter.replaceMarkdownEmojisIn(markdown: String): String {
  return replaceMarkdownEmojisIn(markdown.asSequence())
}

/**
 * This is the longest possible alias length, so we can use its length for our aliasBuilder var
 * below to reuse it and never have to resize it.
 */
private const val MAX_ALIAS_LENGTH = "south_georgia_south_sandwich_islands".length

/**
 * Returns a [Sequence<Char>][Sequence] that replaces occurrences of markdown emojis with android
 * render-able emojis.
 */
private suspend fun EmojiMarkdownConverter.replaceMarkdownEmojisIn(
  markdown: Sequence<Char>
): String {
  val aliasBuilder = StringBuilder(MAX_ALIAS_LENGTH)
  var startAlias = false
  return flow {
      for (char in markdown) {
        if (startAlias || aliasBuilder.isNotEmpty()) {
          if (startAlias && char == ':') {
            // Double ::, so emit a colon and keep startAlias set
            emit(':')
            continue
          }
          startAlias = false
          when (char) {
            ' ' -> {
              // Aliases can't have spaces, so bomb out and restart
              emit(':')
              emitAll(aliasBuilder.asSequence().asFlow())
              emit(' ')
              aliasBuilder.setLength(0)
            }
            ':' -> {
              val potentialAlias = aliasBuilder.toString()
              val potentialEmoji = convert(potentialAlias)
              // If we find an emoji append it and reset alias start, if we don't find an emoji
              // append between the potential start and this index *and* consider this index the new
              // potential start.
              if (potentialEmoji != null) {
                emitAll(potentialEmoji.asSequence().asFlow())
              } else {
                emit(':')
                emitAll(potentialAlias.asSequence().asFlow())
                // Start a new alias from this colon as we didn't have a match with the existing
                // close
                startAlias = true
              }
              aliasBuilder.setLength(0)
            }
            else -> aliasBuilder.append(char)
          }
        } else {
          if (char == ':') {
            startAlias = true
          } else {
            emit(char)
          }
        }
      }

      // If we started an alias but ran out of characters, flush it
      if (startAlias) {
        emit(':')
      } else if (aliasBuilder.isNotEmpty()) {
        emit(':')
        emitAll(aliasBuilder.asSequence().asFlow())
      }
    }
    .collectToString()
}

private suspend fun <T> Flow<T>.collectToString(): String {
  val builder = StringBuilder(MAX_ALIAS_LENGTH)
  collect(builder::append)
  return builder.toString()
}
