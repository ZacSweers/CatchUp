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
package io.sweers.catchup.ui.bugreport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.mattprecious.telescope.Lens
import com.uber.autodispose.autoDispose
import dev.zacsweers.catchup.appconfig.AppConfig
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BaseActivity
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.bugreport.BugReportDialog.ReportListener
import io.sweers.catchup.ui.bugreport.BugReportView.Report
import io.sweers.catchup.util.buildMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Pops a dialog asking for more information about the bug report and then creates an upload with a
 * markdown-formatted body.
 */
@PerActivity
internal class BugReportLens @Inject constructor(
  private val activity: ComponentActivity,
  private val lumberYard: LumberYard,
  private val imgurUploadApi: ImgurUploadApi,
  private val gitHubIssueApi: GitHubIssueApi,
  private val appConfig: AppConfig
) : Lens(), ReportListener {

  private var screenshot: File? = null

  override fun onCapture(screenshot: File?) {
    this.screenshot = screenshot

    val dialog = BugReportDialog(activity)
    dialog.setReportListener(this)
    dialog.show()
  }

  override fun onBugReportSubmit(report: Report) {
    if (report.includeLogs) {
      activity.lifecycleScope.launch {
        try {
          val logs = withContext(Dispatchers.IO) { lumberYard.save() }
          submitReport(report, logs)
        } catch (e: Exception) {
          Toast.makeText(activity, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show()
          submitReport(report, null)
        }
      }
    } else {
      submitReport(report, null)
    }
  }

  private fun submitReport(report: Report, logs: File?) {
    val dm = activity.resources.displayMetrics
    val densityBucket = getDensityString(dm)

    val markdown = buildMarkdown {
      text("Reported by @${report.username}")
      newline(2)
      if (!report.description.isBlank()) {
        h4("Description")
        newline()
        codeBlock(report.description)
        newline(2)
      }
      h4("App")
      codeBlock(buildString {
        append("Version: ").append(appConfig.versionName).append('\n')
        append("Version code: ").append(appConfig.versionCode).append('\n')
        append("Version timestamp: ").append(appConfig.timestamp).append('\n')
      })
      newline()
      h4("Device details")
      codeBlock(buildString {
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
        append("Release: ").append(VERSION.RELEASE).append('\n')
        append("API: ").append(VERSION.SDK_INT).append('\n')
      })
    }
    val body = StringBuilder()
    body.append(markdown)

    uploadIssue(report, body, logs)
  }

  private fun uploadIssue(report: Report, body: StringBuilder, logs: File?) {
    val channelId = "bugreports"
    val notificationManager = activity.getSystemService<NotificationManager>()
        ?: throw IllegalStateException("No notificationmanager?")
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      val channels = notificationManager.notificationChannels
      if (channels.none { it.id == channelId }) {
        NotificationChannel(
            channelId, "Bug reports", NotificationManager.IMPORTANCE_HIGH)
            .apply {
              description = "This is the channel for uploading bug reports. Debug only."
            }
            .let {
              notificationManager.createNotificationChannel(it)
            }
      }
    }

    val notificationId = activity.getString(R.string.app_name).hashCode()
    val notificationBuilder = NotificationCompat.Builder(activity, channelId)
        .apply {
          setSmallIcon(android.R.drawable.stat_sys_upload)
          color = ContextCompat.getColor(activity, R.color.colorAccent)
          setContentTitle("Uploading bug report")
          setAutoCancel(true)
          setProgress(0, 0, true)
          setTicker("Upload in progress")
          setOngoing(true)
          setOnlyAlertOnce(true)
        }

    val finalScreenshot = screenshot
    val screenshotStringStream = if (report.includeScreenshot && finalScreenshot != null) {
      imgurUploadApi
          .postImage(MultipartBody.Part.createFormData(
              "image",
              finalScreenshot.name,
              finalScreenshot.asRequestBody("image/*".toMediaTypeOrNull())
          ))
          .map { "\n\n!${buildMarkdown { link(it, "Screenshot") }}" }
    } else Single.just("\n\nNo screenshot provided")

    screenshotStringStream
        .map { screenshotText ->
          body.append(screenshotText)
          val screenshotMarkdown = buildMarkdown {
            newline(2)
            h4("Logs")
            if (report.includeLogs && logs != null) {
              codeBlock(logs.readText())
            } else {
              text("No logs provided")
            }
          }
          body.append(screenshotMarkdown)
          body.toString()
        }
        .flatMap { bodyText ->
          gitHubIssueApi.createIssue(
              GitHubIssue(report.title, bodyText)
          )
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { notificationManager.notify(notificationId, notificationBuilder.build()) }
        .doOnDispose {
          NotificationCompat.Builder(activity, channelId)
              .apply {
                setSmallIcon(R.drawable.ic_error_black_24dp)
                color = ContextCompat.getColor(activity, R.color.colorAccent)
                setContentTitle("Upload canceled")
                setContentInfo("Probably because the activity was killed ¯\\_(ツ)_/¯")
                setAutoCancel(true)
              }
              .let { notificationManager.notify(notificationId, it.build()) }
        }
        .autoDispose(activity as BaseActivity)
        .subscribe { issueUrl, error ->
          issueUrl?.let {
            NotificationCompat.Builder(activity, channelId)
                .apply {
                  setSmallIcon(R.drawable.ic_check_black_24dp)
                  color = ContextCompat.getColor(activity, R.color.colorAccent)
                  setContentTitle("Bug report successfully uploaded")
                  setContentText(it)
                  val uri = it.toUri()
                  val resultIntent = Intent(Intent.ACTION_VIEW, uri)
                  setContentIntent(PendingIntent.getActivity(activity, 0, resultIntent, 0))
                  setAutoCancel(true)

                  val shareIntent = Intent(Intent.ACTION_SEND, uri)
                  shareIntent.type = "text/plain"
                  shareIntent.putExtra(Intent.EXTRA_TEXT, it)
                  shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                  addAction(NotificationCompat.Action(R.drawable.ic_share_black_24dp,
                      "Share link",
                      PendingIntent.getActivity(activity, 0, shareIntent, 0)))
                }
                .let {
                  notificationManager.notify(notificationId, it.build())
                }
          }
          error?.let {
            NotificationCompat.Builder(activity, channelId)
                .apply {
                  setSmallIcon(R.drawable.ic_error_black_24dp)
                  color = ContextCompat.getColor(activity, R.color.colorAccent)
                  setContentTitle("Upload failed")
                  setContentInfo(
                      "Bug report upload failed. Please try again. If problem persists, take consolation in knowing you got farther than I did.")
                  setAutoCancel(true)
                }
                .let { notificationManager.notify(notificationId, it.build()) }
          }
        }
  }

  private fun getDensityString(displayMetrics: DisplayMetrics) =
      when (val dpi = displayMetrics.densityDpi) {
        DisplayMetrics.DENSITY_LOW -> "ldpi"
        DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
        DisplayMetrics.DENSITY_HIGH -> "hdpi"
        DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
        DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
        DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
        DisplayMetrics.DENSITY_TV -> "tvdpi"
        else -> dpi.toString()
      }
}
