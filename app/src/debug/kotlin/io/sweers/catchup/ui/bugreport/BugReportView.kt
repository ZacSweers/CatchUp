/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.bugreport

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.jakewharton.rxbinding2.widget.RxTextView
import io.sweers.catchup.R
import kotterknife.bindView

class BugReportView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
  private val usernameView by bindView<EditText>(R.id.username)
  private val titleView by bindView<EditText>(R.id.title)
  private val descriptionView by bindView<EditText>(R.id.description)
  private val screenshotView by bindView<CheckBox>(R.id.screenshot)
  private val logsView by bindView<CheckBox>(R.id.logs)

  interface ReportDetailsListener {
    fun onStateChanged(valid: Boolean)
  }

  private var listener: ReportDetailsListener? = null

  override fun onFinishInflate() {
    super.onFinishInflate()
    usernameView.setOnFocusChangeListener { _, hasFocus ->
      if (!hasFocus) {
        usernameView.error = if (usernameView.text.isNullOrBlank()) "Cannot be empty." else null
      }
    }
    titleView.setOnFocusChangeListener { _, hasFocus ->
      if (!hasFocus) {
        titleView.error = if (titleView.text.isNullOrBlank()) "Cannot be empty." else null
      }
    }
    RxTextView.afterTextChangeEvents(titleView)
        .mergeWith(RxTextView.afterTextChangeEvents(usernameView))
        .subscribe {
          val titleIsBlank = titleView.text.isNullOrBlank()
          val userNameIsBlank = usernameView.text.isNullOrBlank()
          listener?.onStateChanged(!titleIsBlank && !userNameIsBlank)
        }

    screenshotView.isChecked = true
    logsView.isChecked = true
  }

  fun setBugReportListener(listener: ReportDetailsListener) {
    this.listener = listener
  }

  val report: Report
    get() = Report(usernameView.text.toString().trim().removePrefix("@"),
        titleView.text.toString(),
        descriptionView.text.toString(),
        screenshotView.isChecked,
        logsView.isChecked)

  data class Report(val username: String,
      val title: String,
      val description: String,
      val includeScreenshot: Boolean,
      val includeLogs: Boolean)
}
