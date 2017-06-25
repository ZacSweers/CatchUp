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

package io.sweers.catchup.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.widget.RxTextView
import com.uber.autodispose.android.ViewScopeProvider
import com.uber.autodispose.kotlin.autoDisposeWith
import io.sweers.catchup.R
import io.sweers.catchup.util.Strings

class BugReportView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
  @BindView(R.id.title) lateinit var titleView: EditText
  @BindView(R.id.description) lateinit var descriptionView: EditText
  @BindView(R.id.screenshot) lateinit var screenshotView: CheckBox
  @BindView(R.id.logs) lateinit var logsView: CheckBox

  interface ReportDetailsListener {
    fun onStateChanged(valid: Boolean)
  }

  private var listener: ReportDetailsListener? = null

  override fun onFinishInflate() {
    super.onFinishInflate()
    ButterKnife.bind(this)

    titleView.setOnFocusChangeListener { _, hasFocus ->
      if (!hasFocus) {
        titleView.error = if (Strings.isBlank(titleView.text)) "Cannot be empty." else null
      }
    }
    RxTextView.afterTextChangeEvents(titleView)
        .autoDisposeWith(ViewScopeProvider.from(this))
        .subscribe { s ->
          listener?.onStateChanged(!Strings.isBlank(s.editable()))
        }

    screenshotView.isChecked = true
    logsView.isChecked = true
  }

  fun setBugReportListener(listener: ReportDetailsListener) {
    this.listener = listener
  }

  val report: Report
    get() = Report(titleView.text.toString(),
        descriptionView.text.toString(), screenshotView.isChecked,
        logsView.isChecked)

  class Report(val title: String, val description: String, val includeScreenshot: Boolean,
      val includeLogs: Boolean)
}
