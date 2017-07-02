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

@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util

inline infix fun String.truncateAt(length: Int): String {
  return if (length > length) substring(0, length) else this
}

/**
 * Unescapes a string that contains standard Java escape sequences.
 *
 *  * **&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'** :
 * BS, FF, NL, CR, TAB, double and single quote.
 *  * **&#92;X &#92;XX &#92;XXX** : Octal character
 * specification (0 - 377, 0x00 - 0xFF).
 *  * **&#92;uXXXX** : Hexadecimal based Unicode character.
 *
 * From [Here](https://gist.github.com/uklimaschewski/6741769)
 *
 * @return The translated string.
 */
inline fun String.unescapeJavaString(): String {
  if (isNullOrBlank()) {
    return this
  }

  val sb = StringBuilder(length)

  var i = 0
  loop@ while (i < length) {
    var ch = this[i]
    if (ch == '\\') {
      val nextChar = if (i == length - 1) '\\' else this[i + 1]
      // Octal escape?
      if (nextChar in '0'..'7') {
        var code = "" + nextChar
        i++
        if (i < length - 1 && this[i + 1] >= '0' && this[i + 1] <= '7') {
          code += this[i + 1]
          i++
          if (i < length - 1 && this[i + 1] >= '0' && this[i + 1] <= '7') {
            code += this[i + 1]
            i++
          }
        }
        sb.append(Integer.parseInt(code, 8).toChar())
        i++
        continue
      }
      when (nextChar) {
        '\\' -> ch = '\\'
        'b' -> ch = '\b'
//        'f' -> ch = '\f'
        'n' -> ch = '\n'
        'r' -> ch = '\r'
        't' -> ch = '\t'
        '\"' -> ch = '\"'
        '\'' -> ch = '\''
      // Hex Unicode: u????
        'u' -> {
          if (i >= length - 5) {
//            ch = 'u'
            break@loop
          }
          val code = Integer.parseInt(""
              + this[i + 2]
              + this[i + 3]
              + this[i + 4]
              + this[i + 5], 16)
          sb.append(Character.toChars(code))
          i += 5
          i++
          continue@loop
        }
      }
      i++
    }
    sb.append(ch)
    i++
  }
  return sb.toString()
}
