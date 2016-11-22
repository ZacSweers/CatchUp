package io.sweers.catchup.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.jakewharton.rxbinding.widget.RxTextView;
import io.sweers.catchup.R;
import io.sweers.catchup.util.Strings;

public final class BugReportView extends LinearLayout {
  @BindView(R.id.title) EditText titleView;
  @BindView(R.id.description) EditText descriptionView;
  @BindView(R.id.screenshot) CheckBox screenshotView;
  @BindView(R.id.logs) CheckBox logsView;

  public interface ReportDetailsListener {
    void onStateChanged(boolean valid);
  }

  private ReportDetailsListener listener;

  public BugReportView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);

    titleView.setOnFocusChangeListener((v, hasFocus) -> {
      if (!hasFocus) {
        titleView.setError(Strings.isBlank(titleView.getText()) ? "Cannot be empty." : null);
      }
    });
    RxTextView.afterTextChangeEvents(titleView).subscribe(s -> {
      if (listener != null) {
        listener.onStateChanged(!Strings.isBlank(s.editable()));
      }
    });

    screenshotView.setChecked(true);
    logsView.setChecked(true);
  }

  public void setBugReportListener(ReportDetailsListener listener) {
    this.listener = listener;
  }

  public Report getReport() {
    return new Report(String.valueOf(titleView.getText()),
        String.valueOf(descriptionView.getText()), screenshotView.isChecked(),
        logsView.isChecked());
  }

  public static final class Report {
    public final String title;
    public final String description;
    public final boolean includeScreenshot;
    public final boolean includeLogs;

    public Report(String title, String description, boolean includeScreenshot,
        boolean includeLogs) {
      this.title = title;
      this.description = description;
      this.includeScreenshot = includeScreenshot;
      this.includeLogs = includeLogs;
    }
  }
}
