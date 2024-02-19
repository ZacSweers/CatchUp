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
package catchup.app.ui.bugreport

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import catchup.flowbinding.viewScope
import dev.zacsweers.catchup.app.scaffold.databinding.BugreportViewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.ldralighieri.corbind.view.focusChanges
import ru.ldralighieri.corbind.widget.afterTextChangeEvents

class BugReportView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
  private lateinit var binding: BugreportViewBinding
  private val usernameView
    get() = binding.username

  private val titleView
    get() = binding.title

  private val descriptionView
    get() = binding.description

  private val screenshotView
    get() = binding.screenshot

  private val logsView
    get() = binding.logs

  interface ReportDetailsListener {
    fun onStateChanged(valid: Boolean)
  }

  private var listener: ReportDetailsListener? = null

  override fun onFinishInflate() {
    super.onFinishInflate()
    binding = BugreportViewBinding.bind(this)
    viewScope().launch {
      usernameView.focusChanges().drop(1).collect { hasFocus ->
        if (!hasFocus) {
          usernameView.error = if (usernameView.text.isNullOrBlank()) "Cannot be empty." else null
        }
      }
      titleView.focusChanges().drop(1).collect { hasFocus ->
        if (!hasFocus) {
          titleView.error = if (titleView.text.isNullOrBlank()) "Cannot be empty." else null
        }
      }
      combine(
          titleView.afterTextChangeEvents().map { !it.editable.isNullOrBlank() },
          usernameView.afterTextChangeEvents().map { !it.editable.isNullOrBlank() },
          transform = { title, username -> title && username },
        )
        .onEach { listener?.onStateChanged(it) }
        .collect()
    }

    screenshotView.isChecked = true
    logsView.isChecked = true
  }

  fun setBugReportListener(listener: ReportDetailsListener) {
    this.listener = listener
  }

  val report: Report
    get() =
      Report(
        usernameView.text.toString().trim().removePrefix("@"),
        titleView.text.toString(),
        descriptionView.text.toString(),
        screenshotView.isChecked,
        logsView.isChecked,
      )

  data class Report(
    val username: String,
    val title: String,
    val description: String,
    val includeScreenshot: Boolean,
    val includeLogs: Boolean,
  )
}
