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

package io.sweers.catchup.util

import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
import java.util.ArrayDeque
import java.util.Deque

/** A [SpannableStringBuilder] wrapper whose API doesn't make me want to stab my eyes out.  */
class Truss {

  private val builder: SpannableStringBuilder = SpannableStringBuilder()
  private val stack: Deque<Span>

  init {
    stack = ArrayDeque()
  }

  operator fun plusAssign(string: String) {
    append(string)
  }

  fun append(string: String): Truss {
    builder.append(string)
    return this
  }

  operator fun plusAssign(charSequence: CharSequence) {
    append(charSequence)
  }

  fun append(charSequence: CharSequence): Truss {
    builder.append(charSequence)
    return this
  }

  operator fun plusAssign(c: Char) {
    append(c)
  }

  fun append(c: Char): Truss {
    builder.append(c)
    return this
  }

  operator fun plusAssign(number: Number) {
    append(number)
  }

  fun append(number: Number): Truss {
    builder.append(number.toString())
    return this
  }

  fun surround(input: CharSequence, span: () -> Any): Truss {
    return pushSpan(span())
        .append(input)
        .popSpan()
  }

  /** Starts [span] at the current position in the builder. */
  fun pushSpan(span: Any): Truss {
    stack.addLast(Span(builder.length, span))
    return this
  }

  /** End the most recently pushed span at the current position in the builder. */
  fun popSpan(): Truss {
    val span = stack.removeLast()
    builder.setSpan(span.span, span.start, builder.length, SPAN_INCLUSIVE_EXCLUSIVE)
    return this
  }

  /** Create the final [CharSequence], popping any remaining spans. */
  fun build(): CharSequence {
    while (!stack.isEmpty()) {
      popSpan()
    }
    return SpannableStringBuilder(builder)
  }

  private class Span(internal val start: Int, internal val span: Any)
}
