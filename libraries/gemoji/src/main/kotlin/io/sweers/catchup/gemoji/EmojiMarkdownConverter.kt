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
package io.sweers.catchup.gemoji

/**
 * Converts a markdown emoji alias (eg, ":smile:") into an android render-able emoji.
 */
interface EmojiMarkdownConverter {
  fun convert(alias: String): String?
}

internal class GemojiEmojiMarkdownConverter(
  private val gemojiDao: GemojiDao
) : EmojiMarkdownConverter {
  override fun convert(alias: String): String? {
    return gemojiDao.getEmoji(alias)
  }
}

fun Sequence<Char>.asString(): String {
  return buildString {
    this@asString.forEach { append(it) }
  }
}

fun Sequence<Char>.asString(capacity: Int): String {
  return buildString(capacity) {
    this@asString.forEach { append(it) }
  }
}

/**
 * Returns a [String] that replaces occurrences of markdown emojis with android render-able emojis.
 */
fun EmojiMarkdownConverter.replaceMarkdownEmojisIn(markdown: String): String {
  return replaceMarkdownEmojisIn(markdown.asSequence()).asString(markdown.length)
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
fun EmojiMarkdownConverter.replaceMarkdownEmojisIn(markdown: Sequence<Char>): Sequence<Char> {
  val aliasBuilder = StringBuilder(MAX_ALIAS_LENGTH)
  var startAlias = false
  return sequence {
    markdown.forEach { char ->
      if (startAlias || aliasBuilder.isNotEmpty()) {
        if (startAlias && char == ':') {
          // Double ::, so emit a colon and keep startAlias set
          yield(':')
          return@forEach
        }
        startAlias = false
        when (char) {
          ' ' -> {
            // Aliases can't have spaces, so bomb out and restart
            yield(':')
            yieldAll(aliasBuilder.asSequence())
            yield(' ')
            aliasBuilder.setLength(0)
          }
          ':' -> {
            val potentialAlias = aliasBuilder.toString()
            val potentialEmoji = convert(potentialAlias)
            // If we find an emoji append it and reset alias start, if we don't find an emoji
            // append between the potential start and this index *and* consider this index the new
            // potential start.
            if (potentialEmoji != null) {
              yieldAll(potentialEmoji.asSequence())
            } else {
              yield(':')
              yieldAll(potentialAlias.asSequence())
              // Start a new alias from this colon as we didn't have a match with the existing close
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
          yield(char)
        }
      }
    }

    // If we started an alias but ran out of characters, flush it
    if (startAlias) {
      yield(':')
    } else if (aliasBuilder.isNotEmpty()) {
      yield(':')
      yieldAll(aliasBuilder.asSequence())
    }
  }
}
