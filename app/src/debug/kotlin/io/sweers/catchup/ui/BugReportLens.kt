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

import android.app.Activity
import android.os.Build
import android.support.v4.app.ShareCompat
import android.util.DisplayMetrics
import android.widget.Toast
import com.mattprecious.telescope.Lens
import com.mattprecious.telescope.TelescopeFileProvider
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.BugReportDialog.ReportListener
import io.sweers.catchup.ui.BugReportView.Report
import io.sweers.catchup.util.maybeStartActivity
import java.io.File
import javax.inject.Inject

/**
 * Pops a dialog asking for more information about the bug report and then creates an email with a
 * JIRA-formatted body.
 */
@PerActivity
class BugReportLens @Inject constructor(private val activity: Activity,
    private val lumberYard: LumberYard) : Lens(), ReportListener {

  private var screenshot: File? = null

  override fun onCapture(screenshot: File?) {
    this.screenshot = screenshot

    val dialog = BugReportDialog(activity)
    dialog.setReportListener(this)
    dialog.show()
  }

  override fun onBugReportSubmit(report: Report) {
    if (report.includeLogs) {
      lumberYard.save()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(object : SingleObserver<File> {
            override fun onSubscribe(d: Disposable) {

            }

            override fun onSuccess(logs: File) {
              submitReport(report, logs)
            }

            override fun onError(e: Throwable) {
              Toast.makeText(activity, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show()
              submitReport(report, null)
            }
          })
    } else {
      submitReport(report, null)
    }
  }

  private fun submitReport(report: Report, logs: File?) {
    val dm = activity.resources.displayMetrics
    val densityBucket = getDensityString(dm)

    val intent = ShareCompat.IntentBuilder.from(activity).setType("message/rfc822")
        // TODO: .addEmailTo("u2020-bugs@blackhole.io")
        .setSubject(report.title)

    val body = StringBuilder()
    if (!report.description.isBlank()) {
      body.append("{panel:title=Description}\n").append(report.description).append("\n{panel}\n\n")
    }

    body.run {
      append("{panel:title=App}\n")
      append("Version: ").append(BuildConfig.VERSION_NAME).append('\n')
      append("Version code: ").append(BuildConfig.VERSION_CODE).append('\n')
      append("{panel}\n\n")

      append("{panel:title=Device}\n")
      append("Make: ").append(Build.MANUFACTURER).append('\n')
      append("Model: ").append(Build.MODEL).append('\n')
      append("Resolution: ")
          .append(dm.heightPixels)
          .append("x")
          .append(dm.widthPixels)
          .append('\n')
      append("Density: ")
          .append(dm.densityDpi)
          .append("dpi (")
          .append(densityBucket)
          .append(")\n")
      append("Release: ").append(Build.VERSION.RELEASE).append('\n')
      append("API: ").append(Build.VERSION.SDK_INT).append('\n')
      append("{panel}")
    }

    intent.setText(body.toString())

    if (screenshot != null && report.includeScreenshot) {
      intent.addStream(TelescopeFileProvider
          .getUriForFile(activity.applicationContext, screenshot))
    }
    if (logs != null) {
      intent.addStream(TelescopeFileProvider
          .getUriForFile(activity.applicationContext, logs))
    }

    activity.maybeStartActivity(intent.intent)
  }

  private fun getDensityString(displayMetrics: DisplayMetrics): String {
    return when (displayMetrics.densityDpi) {
      DisplayMetrics.DENSITY_LOW -> "ldpi"
      DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
      DisplayMetrics.DENSITY_HIGH -> "hdpi"
      DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
      DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
      DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
      DisplayMetrics.DENSITY_TV -> "tvdpi"
      else -> displayMetrics.densityDpi.toString()
    }
  }
}
