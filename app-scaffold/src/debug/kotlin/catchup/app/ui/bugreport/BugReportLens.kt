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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import catchup.app.data.DiskLumberYard
import catchup.app.data.LumberYard
import catchup.app.ui.bugreport.BugReportDialog.ReportListener
import catchup.app.ui.bugreport.BugReportView.Report
import catchup.app.util.buildMarkdown
import catchup.appconfig.AppConfig
import com.mattprecious.telescope.Lens
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.app.scaffold.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Pops a dialog asking for more information about the bug report and then creates an upload with a
 * markdown-formatted body.
 */
class BugReportLens
@AssistedInject
constructor(
  @Assisted private val activity: ComponentActivity,
  private val lumberYard: LumberYard,
  private val imgurUploadApi: ImgurUploadApi,
  private val gitHubIssueApi: GitHubIssueApi,
  private val appConfig: AppConfig,
) : Lens(), ReportListener {

  private var screenshot: File? = null

  override fun onCapture(screenshot: File?) {
    this.screenshot = screenshot

    val dialog = BugReportDialog(activity, activity.lifecycleScope)
    dialog.setReportListener(this)
    dialog.show()
  }

  override suspend fun onBugReportSubmit(context: Context, report: Report) {
    withContext(Dispatchers.IO) {
      if (report.includeLogs) {
        try {
          lumberYard.flush()
          submitReport(context, report) {
            if (lumberYard is DiskLumberYard) {
              lumberYard.currentLogFileText()
            } else {
              lumberYard
                .bufferedLogs()
                .joinToString("\n", transform = LumberYard.Entry::prettyPrint)
            }
          }
        } catch (e: Exception) {
          Toast.makeText(context, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show()
          submitReport(context, report, null)
        }
      } else {
        submitReport(context, report, null)
      }
    }
  }

  private suspend fun submitReport(
    context: Context,
    report: Report,
    logs: (suspend () -> String)?,
  ) {
    val displayMetrics = context.resources.displayMetrics
    val densityBucket = getDensityString(displayMetrics)

    val markdown = buildMarkdown {
      text("Reported by @${report.username}")
      newline(2)
      if (report.description.isNotBlank()) {
        h4("Description")
        newline()
        codeBlock(report.description)
        newline(2)
      }
      h4("App")
      codeBlock(
        buildString {
          append("Version: ").append(appConfig.versionName).append('\n')
          append("Version code: ").append(appConfig.versionCode).append('\n')
          append("Version timestamp: ").append(appConfig.timestamp).append('\n')
        }
      )
      newline()
      h4("Device details")
      codeBlock(
        buildString {
          append("Make: ").append(Build.MANUFACTURER).append('\n')
          append("Model: ").append(Build.MODEL).append('\n')
          append("Resolution: ")
            .append(displayMetrics.heightPixels)
            .append("x")
            .append(displayMetrics.widthPixels)
            .append('\n')
          append("Density: ")
            .append(displayMetrics.densityDpi)
            .append("dpi (")
            .append(densityBucket)
            .append(")\n")
          append("Release: ").append(VERSION.RELEASE).append('\n')
          append("API: ").append(appConfig.sdkInt).append('\n')
        }
      )
    }
    val body = StringBuilder()
    body.append(markdown)

    uploadIssue(context, report, body, logs)
  }

  private suspend fun uploadIssue(
    context: Context,
    report: Report,
    body: StringBuilder,
    logs: (suspend () -> String)?,
  ) {
    val channelId = "bugreports"
    val notificationManager =
      context.getSystemService<NotificationManager>()
        ?: throw IllegalStateException("No notificationmanager?")
    val channels = notificationManager.notificationChannels
    if (channels.none { it.id == channelId }) {
      NotificationChannel(channelId, "Bug reports", NotificationManager.IMPORTANCE_HIGH)
        .apply { description = "This is the channel for uploading bug reports. Debug only." }
        .let { notificationManager.createNotificationChannel(it) }
    }

    val notificationId = context.getString(R.string.app_name).hashCode()
    val notificationBuilder =
      NotificationCompat.Builder(context, channelId).apply {
        setSmallIcon(android.R.drawable.stat_sys_upload)
        color = ContextCompat.getColor(context, R.color.colorAccent)
        setContentTitle("Uploading bug report")
        setAutoCancel(true)
        setProgress(0, 0, true)
        setTicker("Upload in progress")
        setOngoing(true)
        setOnlyAlertOnce(true)
      }

    val finalScreenshot = screenshot
    val screenshotText =
      if (report.includeScreenshot && finalScreenshot != null) {
        imgurUploadApi
          .postImage(
            MultipartBody.Part.createFormData(
              "image",
              finalScreenshot.name,
              finalScreenshot.asRequestBody("image/*".toMediaTypeOrNull()),
            )
          )
          .let { "\n\n!${buildMarkdown { link(it, "Screenshot") }}" }
      } else {
        "\n\nNo screenshot provided"
      }

    val screenshotMarkdown = buildMarkdown {
      newline(2)
      h4("Logs")
      if (report.includeLogs && logs != null) {
        codeBlock(logs())
      } else {
        text("No logs provided")
      }
    }
    val bodyText =
      with(body) {
        append(screenshotText)
        append(screenshotMarkdown)
        toString()
      }

    try {
      withContext(Dispatchers.Main) {
        notificationManager.notify(notificationId, notificationBuilder.build())
      }
      // TODO change to eithernet
      val issueUrl = gitHubIssueApi.createIssue(GitHubIssue(report.title, bodyText))
      NotificationCompat.Builder(context, channelId)
        .apply {
          setSmallIcon(R.drawable.ic_check_black_24dp)
          color = ContextCompat.getColor(context, R.color.colorAccent)
          setContentTitle("Bug report successfully uploaded")
          setContentText(issueUrl)
          val uri = issueUrl.toUri()
          val resultIntent = Intent(Intent.ACTION_VIEW, uri)
          setContentIntent(
            PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE)
          )
          setAutoCancel(true)

          val shareIntent = Intent(Intent.ACTION_SEND, uri)
          shareIntent.setDataAndType(null, "text/plain")
          shareIntent.putExtra(Intent.EXTRA_TEXT, issueUrl)
          shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

          addAction(
            NotificationCompat.Action(
              R.drawable.ic_share_black_24dp,
              "Share link",
              PendingIntent.getActivity(context, 0, shareIntent, PendingIntent.FLAG_IMMUTABLE),
            )
          )
        }
        .let { notificationManager.notify(notificationId, it.build()) }
    } catch (e: Exception) {
      withContext(Dispatchers.Main) {
        NotificationCompat.Builder(context, channelId)
          .apply {
            setSmallIcon(R.drawable.ic_error_black_24dp)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            setContentTitle("Upload failed")
            setContentInfo(
              "Bug report upload failed. Please try again. If problem persists, take consolation in knowing you got farther than I did."
            )
            setAutoCancel(true)
          }
          .let { notificationManager.notify(notificationId, it.build()) }
      }
    } finally {
      withContext(Dispatchers.Main) {
        NotificationCompat.Builder(context, channelId)
          .apply {
            setSmallIcon(R.drawable.ic_error_black_24dp)
            color = ContextCompat.getColor(context, R.color.colorAccent)
            setContentTitle("Upload canceled")
            setContentInfo("Probably because the activity was killed ¯\\_(ツ)_/¯")
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

  @AssistedFactory
  fun interface Factory {
    fun create(activity: ComponentActivity): BugReportLens
  }
}
