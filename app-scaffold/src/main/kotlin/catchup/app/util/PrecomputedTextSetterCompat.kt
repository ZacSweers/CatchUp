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
package catchup.app.util

import android.text.Spanned
import android.widget.TextView
import androidx.core.text.PrecomputedTextCompat
import catchup.flowbinding.viewScope
import io.noties.markwon.Markwon
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Please do not use with `markwon-recycler` as it will lead to bad item rendering (due to async
 * nature)
 */
class PrecomputedTextSetterCompat internal constructor(private val context: CoroutineContext) :
  Markwon.TextSetter {

  override fun setText(
    textView: TextView,
    markdown: Spanned,
    bufferType: TextView.BufferType,
    onComplete: Runnable
  ) {
    textView.viewScope().launch {
      try {
        val precomputedTextCompat = withContext(context) { precomputedText(textView, markdown) }
        if (precomputedTextCompat != null) {
          applyText(textView, precomputedTextCompat, bufferType, onComplete)
        } else {
          applyText(textView, markdown, bufferType, onComplete)
        }
      } catch (t: Throwable) {
        Timber.tag("PrecomputdTxtSetterCmpt").e(t, "Exception during pre-computing text")
        // apply initial markdown
        applyText(textView, markdown, bufferType, onComplete)
      }
    }
  }

  companion object {

    /** @param context for background execution of text pre-computation */
    fun create(
      context: CoroutineContext = Dispatchers.Default,
    ): PrecomputedTextSetterCompat {
      return PrecomputedTextSetterCompat(context)
    }

    private fun precomputedText(
      textView: TextView,
      spanned: Spanned,
    ): PrecomputedTextCompat? {

      // use native parameters on P
      val params: PrecomputedTextCompat.Params =
        PrecomputedTextCompat.Params(textView.textMetricsParams)

      return PrecomputedTextCompat.create(spanned, params)
    }

    private fun applyText(
      textView: TextView,
      text: Spanned,
      bufferType: TextView.BufferType,
      onComplete: Runnable
    ) {
      textView.setText(text, bufferType)
      onComplete.run()
    }
  }
}
