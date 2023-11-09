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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import catchup.app.ui.bugreport.BugReportView.Report
import catchup.app.ui.bugreport.BugReportView.ReportDetailsListener
import dev.zacsweers.catchup.app.scaffold.databinding.BugreportViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BugReportDialog(context: Context, scope: CoroutineScope) :
  AlertDialog(context), ReportDetailsListener {
  interface ReportListener {
    suspend fun onBugReportSubmit(context: Context, report: Report)
  }

  private var listener: ReportListener? = null

  init {
    val view = BugreportViewBinding.inflate(LayoutInflater.from(context))
    view.root.setBugReportListener(this)

    setTitle("Report a bug")
    setView(view.root)
    setButton(Dialog.BUTTON_NEGATIVE, "Cancel", null as DialogInterface.OnClickListener?)
    setButton(Dialog.BUTTON_POSITIVE, "Submit") { _, _ ->
      scope.launch { listener?.onBugReportSubmit(context, view.root.report) }
    }
  }

  fun setReportListener(listener: ReportListener) {
    this.listener = listener
  }

  override fun onStart() {
    super.onStart()
    getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
  }

  override fun onStateChanged(valid: Boolean) {
    getButton(Dialog.BUTTON_POSITIVE).isEnabled = valid
  }
}
