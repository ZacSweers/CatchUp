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
package io.sweers.catchup.util

import android.content.res.ColorStateList
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.URLSpan
import android.text.util.Linkify
import android.widget.TextView
import androidx.annotation.ColorInt
import io.noties.markwon.Markwon

/*
 * Utility methods for working with HTML and markdown.
 */

inline class Markdown(val rawValue: String) {
  override fun toString(): String = rawValue
}

fun String.markdown(): Markdown = Markdown(this)

/**
 * Parse Markdown and plain-text links.
 *
 * [Markwon] does not handle plain text links (i.e. not md syntax) and requires a
 * `String` input (i.e. squashes any spans). [Linkify] handles plain links but also
 * removes any existing spans. So we can't just run our input through both.
 *
 * Instead we use the markdown lib, then take a copy of the output and Linkify
 * **that**. We then find any [URLSpan]s and add them to the markdown output.
 * Best of both worlds.
 */
fun Markdown.parseMarkdownAndPlainLinks(
  on: TextView,
  with: Markwon,
  alternateSpans: ((String) -> Set<Any>)? = null
): CharSequence {
  val markdown = with.toMarkdown(rawValue)
  with.setParsedMarkdown(on, markdown)
  return markdown.linkifyPlainLinks(on.linkTextColors, on.highlightColor, alternateSpans)
}

/**
 * Parse Markdown and plain-text links and set on the [TextView] with proper clickable
 * spans.
 */
fun Markdown.parseMarkdownAndSetText(
  textView: TextView,
  markwon: Markwon,
  alternateUrlSpan: ((String) -> Set<Any>)? = null
) {
  if (TextUtils.isEmpty(rawValue)) {
    return
  }
  textView.setTextWithNiceLinks(
      parseMarkdownAndPlainLinks(textView, markwon, alternateUrlSpan))
}

/**
 * Work around some 'features' of TextView and URLSpans. i.e. vanilla URLSpans do not react to
 * touch so we replace them with our own [TouchableUrlSpan]
 * & [LinkTouchMovementMethod] to fix this.
 *
 * Setting a custom MovementMethod on a TextView also alters touch handling (see
 * TextView#fixFocusableAndClickableSettings) so we need to correct this.
 */
fun TextView.setTextWithNiceLinks(input: CharSequence) {
  text = input
  movementMethod = LinkTouchMovementMethod.getInstance()
  isFocusable = false
  isClickable = false
  isLongClickable = false
}

/**
 * Parse the given input using [TouchableUrlSpan]s rather than vanilla [URLSpan]s
 * so that they respond to touch.
 */
fun String.parseHtml(
  linkTextColor: ColorStateList,
  @ColorInt linkHighlightColor: Int
): SpannableStringBuilder {
  var spanned = fromHtml()

  // strip any trailing newlines
  while (spanned[spanned.length - 1] == '\n') {
    spanned = spanned.delete(spanned.length - 1, spanned.length)
  }

  return spanned.linkifyPlainLinks(linkTextColor, linkHighlightColor)
}

fun TextView.parseAndSetText(input: String) {
  if (TextUtils.isEmpty(input)) {
    return
  }
  setTextWithNiceLinks(input.parseHtml(linkTextColors, highlightColor))
}

private fun CharSequence.linkifyPlainLinks(
  linkTextColor: ColorStateList,
  @ColorInt linkHighlightColor: Int,
  alternateUrlSpan: ((String) -> Set<Any>)? = null
): SpannableStringBuilder {
  val plainLinks = SpannableString(this) // copy of this

  // Linkify doesn't seem to work as expected on M+
  // TODO: figure out why
  // Linkify.addLinks(plainLinks, Linkify.WEB_URLS);

  val urlSpans = plainLinks.getSpans(0, plainLinks.length, URLSpan::class.java)

  // add any plain links to the output
  val ssb = SpannableStringBuilder(this)
  for (urlSpan in urlSpans) {
    ssb.removeSpan(urlSpan)
    alternateUrlSpan
        ?.invoke(urlSpan.url)
        ?.forEach { setSpan(ssb, urlSpan, plainLinks, it) }
        ?: setSpan(ssb, urlSpan, plainLinks,
            object : TouchableUrlSpan(urlSpan.url, linkTextColor, linkHighlightColor) {
              override fun onClick(url: String) {
                // TODO wat
              }
            })
  }

  return ssb
}

private fun setSpan(
  ssb: SpannableStringBuilder,
  urlSpan: URLSpan,
  plainLinks: SpannableString,
  span: Any
) {
  ssb.setSpan(span,
      plainLinks.getSpanStart(urlSpan),
      plainLinks.getSpanEnd(urlSpan),
      Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private fun String.fromHtml(): SpannableStringBuilder {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY) as SpannableStringBuilder
  } else {
    @Suppress("DEPRECATION")
    Html.fromHtml(this) as SpannableStringBuilder
  }
}
