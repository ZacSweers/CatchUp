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

package io.sweers.catchup.ui;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.util.DisplayMetrics;
import android.widget.Toast;
import com.mattprecious.telescope.Lens;
import com.mattprecious.telescope.TelescopeFileProvider;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.ui.BugReportDialog.ReportListener;
import io.sweers.catchup.ui.BugReportView.Report;
import io.sweers.catchup.util.Intents;
import io.sweers.catchup.util.Strings;
import java.io.File;

/**
 * Pops a dialog asking for more information about the bug report and then creates an email with a
 * JIRA-formatted body.
 */
public final class BugReportLens extends Lens implements ReportListener {
  private final Activity context;
  private final LumberYard lumberYard;

  private File screenshot;

  public BugReportLens(Activity context, LumberYard lumberYard) {
    this.context = context;
    this.lumberYard = lumberYard;
  }

  @Override public void onCapture(File screenshot) {
    this.screenshot = screenshot;

    BugReportDialog dialog = new BugReportDialog(context);
    dialog.setReportListener(this);
    dialog.show();
  }

  @Override public void onBugReportSubmit(final Report report) {
    if (report.includeLogs) {
      lumberYard.save()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new SingleObserver<File>() {
            @Override public void onSubscribe(Disposable d) {

            }

            @Override public void onSuccess(File logs) {
              submitReport(report, logs);
            }

            @Override public void onError(Throwable e) {
              Toast.makeText(context, "Couldn't attach the logs.", Toast.LENGTH_SHORT).show();
              submitReport(report, null);
            }
          });
    } else {
      submitReport(report, null);
    }
  }

  private void submitReport(Report report, @Nullable File logs) {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    String densityBucket = getDensityString(dm);

    ShareCompat.IntentBuilder intent =
        ShareCompat.IntentBuilder.from(context).setType("message/rfc822")
            // TODO: .addEmailTo("u2020-bugs@blackhole.io")
            .setSubject(report.title);

    StringBuilder body = new StringBuilder();
    if (!Strings.isBlank(report.description)) {
      body.append("{panel:title=Description}\n").append(report.description).append("\n{panel}\n\n");
    }

    body.append("{panel:title=App}\n");
    body.append("Version: ").append(BuildConfig.VERSION_NAME).append('\n');
    body.append("Version code: ").append(BuildConfig.VERSION_CODE).append('\n');
    body.append("{panel}\n\n");

    body.append("{panel:title=Device}\n");
    body.append("Make: ").append(Build.MANUFACTURER).append('\n');
    body.append("Model: ").append(Build.MODEL).append('\n');
    body.append("Resolution: ")
        .append(dm.heightPixels)
        .append("x")
        .append(dm.widthPixels)
        .append('\n');
    body.append("Density: ")
        .append(dm.densityDpi)
        .append("dpi (")
        .append(densityBucket)
        .append(")\n");
    body.append("Release: ").append(Build.VERSION.RELEASE).append('\n');
    body.append("API: ").append(Build.VERSION.SDK_INT).append('\n');
    body.append("{panel}");

    intent.setText(body.toString());

    if (screenshot != null && report.includeScreenshot) {
      intent.addStream(TelescopeFileProvider
          .getUriForFile(context.getApplicationContext(), screenshot));
    }
    if (logs != null) {
      intent.addStream(TelescopeFileProvider
          .getUriForFile(context.getApplicationContext(), logs));
    }

    Intents.maybeStartActivity(context, intent.getIntent());
  }

  private static String getDensityString(DisplayMetrics displayMetrics) {
    switch (displayMetrics.densityDpi) {
      case DisplayMetrics.DENSITY_LOW:
        return "ldpi";
      case DisplayMetrics.DENSITY_MEDIUM:
        return "mdpi";
      case DisplayMetrics.DENSITY_HIGH:
        return "hdpi";
      case DisplayMetrics.DENSITY_XHIGH:
        return "xhdpi";
      case DisplayMetrics.DENSITY_XXHIGH:
        return "xxhdpi";
      case DisplayMetrics.DENSITY_XXXHIGH:
        return "xxxhdpi";
      case DisplayMetrics.DENSITY_TV:
        return "tvdpi";
      default:
        return String.valueOf(displayMetrics.densityDpi);
    }
  }
}
