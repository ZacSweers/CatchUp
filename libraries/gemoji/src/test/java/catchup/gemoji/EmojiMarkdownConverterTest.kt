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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EmojiMarkdownConverterTest {

  companion object {
    const val replaced = "replaced"
    const val emoji = ":emoji:"
  }

  private val converter =
    object : EmojiMarkdownConverter {
      override suspend fun convert(alias: String) = if (alias == "emoji") replaced else null
    }

  @Test
  fun testEmpty() = runTest {
    val converted = converter.replaceMarkdownEmojisIn("")
    assertThat(converted).isEmpty()
  }

  @Test
  fun testSimpleReplace() = runTest {
    val converted = convert(emoji)
    assertThat(converted).isEqualTo(replaced)
  }

  @Test
  fun testSimpleNoReplace() = runTest {
    val converted = convert("emoji")
    assertThat(converted).isEqualTo("emoji")
  }

  @Test
  fun testReplaceOnceWithOtherText() = runTest {
    val converted = convert("other text $emoji")
    assertThat(converted).isEqualTo("other text $replaced")
  }

  @Test
  fun testReplaceOnceWithExtraColons() = runTest {
    var converted = convert(":$emoji")
    assertThat(converted).isEqualTo(":$replaced")

    converted = convert("$emoji:")
    assertThat(converted).isEqualTo("$replaced:")

    converted = convert(":$emoji:")
    assertThat(converted).isEqualTo(":$replaced:")

    converted = convert(":other text $emoji")
    assertThat(converted).isEqualTo(":other text $replaced")

    converted = convert(":text$emoji")
    assertThat(converted).isEqualTo(":text$replaced")

    converted = convert(":$emoji")
    assertThat(converted).isEqualTo(":$replaced")
  }

  @Test
  fun testReplaceMultipleTimes() = runTest {
    var converted = convert("$emoji$emoji")
    assertThat(converted).isEqualTo("$replaced$replaced")

    converted = convert("$emoji other text $emoji")
    assertThat(converted).isEqualTo("$replaced other text $replaced")

    converted = convert("$emoji other : text $emoji")
    assertThat(converted).isEqualTo("$replaced other : text $replaced")

    converted = convert(": $emoji other text $emoji")
    assertThat(converted).isEqualTo(": $replaced other text $replaced")

    converted = convert("$emoji:notEmoji:$emoji")
    assertThat(converted).isEqualTo("$replaced:notEmoji:$replaced")

    converted = convert("$emoji:notEmoji:$emoji:")
    assertThat(converted).isEqualTo("$replaced:notEmoji:$replaced:")
  }

  private suspend fun convert(markdown: String) = converter.replaceMarkdownEmojisIn(markdown)
}
