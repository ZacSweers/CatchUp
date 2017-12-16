/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
    private val gemojiDao: GemojiDao) : EmojiMarkdownConverter {
  override fun convert(alias: String): String? {
    return if (alias.startsWith(':') && alias.endsWith(":")) {
      gemojiDao.getEmoji(alias.substring(1, alias.lastIndex))
    } else {
      null
    }
  }
}

/**
 * Returns a [String] that replaces occurrences of markdown emojis with android render-able emojis.
 */
fun replaceMarkdownEmojis(markdown: String, converter: EmojiMarkdownConverter): String {
  val sb = StringBuilder(markdown.length)
  var potentialAliasStart = -1

  markdown.forEachIndexed { index, char ->
    if (char == ':') {
      potentialAliasStart = if (potentialAliasStart == -1) {
        // If we have no potential start, any : is a potential start
        index
      } else {
        val potentialAlias = markdown.substring(potentialAliasStart, index + 1)
        val potentialEmoji = converter.convert(potentialAlias)
        // If we find an emoji append it and reset alias start, if we don't find an emoji
        // append between the potential start and this index *and* consider this index the new
        // potential start.
        if (potentialEmoji != null) {
          sb.append(potentialEmoji)
          -1
        } else {
          sb.append(markdown, potentialAliasStart, index)
          index
        }
      }
      // While not looking for an alias end append all non possible alias chars to the string
    } else if (potentialAliasStart == -1) {
      sb.append(char)
    }
  }

  // Finished iterating markdown while looking for an end, append anything remaining
  if (potentialAliasStart != -1) {
    sb.append(markdown, potentialAliasStart, markdown.length)
  }

  return sb.toString()
}
