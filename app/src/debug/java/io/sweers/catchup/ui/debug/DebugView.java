package io.sweers.catchup.ui.debug;

import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.f2prateek.rx.preferences.Preference;
import com.jakewharton.processphoenix.ProcessPhoenix;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxAdapterView;
import com.squareup.leakcanary.internal.DisplayLeakActivity;
import dagger.Lazy;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.app.ApplicationComponent;
import io.sweers.catchup.app.CatchUpApplication;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.injection.scopes.PerView;
import io.sweers.catchup.ui.logs.LogsDialog;
import io.sweers.catchup.util.Strings;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.inject.Inject;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.TemporalAccessor;
import retrofit2.mock.NetworkBehavior;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class DebugView extends FrameLayout {
  private static final DateTimeFormatter DATE_DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US)
          .withZone(ZoneId.systemDefault());
  @BindView(R.id.debug_contextual_title) View contextualTitleView;
  @BindView(R.id.debug_contextual_list) LinearLayout contextualListView;
  @BindView(R.id.debug_network_delay) Spinner networkDelayView;
  @BindView(R.id.debug_network_variance) Spinner networkVarianceView;
  @BindView(R.id.debug_network_error) Spinner networkErrorView;
  @BindView(R.id.debug_enable_mock_mode) Switch enableMockModeView;
  @BindView(R.id.debug_ui_animation_speed) Spinner uiAnimationSpeedView;
  @BindView(R.id.debug_ui_pixel_grid) Switch uiPixelGridView;
  @BindView(R.id.debug_ui_pixel_ratio) Switch uiPixelRatioView;
  @BindView(R.id.debug_ui_scalpel) Switch uiScalpelView;
  @BindView(R.id.debug_ui_scalpel_wireframe) Switch uiScalpelWireframeView;
  @BindView(R.id.debug_build_name) TextView buildNameView;
  @BindView(R.id.debug_build_code) TextView buildCodeView;
  @BindView(R.id.debug_build_sha) TextView buildShaView;
  @BindView(R.id.debug_build_date) TextView buildDateView;
  @BindView(R.id.debug_device_make) TextView deviceMakeView;
  @BindView(R.id.debug_device_model) TextView deviceModelView;
  @BindView(R.id.debug_device_resolution) TextView deviceResolutionView;
  @BindView(R.id.debug_device_density) TextView deviceDensityView;
  @BindView(R.id.debug_device_release) TextView deviceReleaseView;
  @BindView(R.id.debug_device_api) TextView deviceApiView;
  @BindView(R.id.debug_okhttp_cache_max_size) TextView okHttpCacheMaxSizeView;
  @BindView(R.id.debug_okhttp_cache_write_error) TextView okHttpCacheWriteErrorView;
  @BindView(R.id.debug_okhttp_cache_request_count) TextView okHttpCacheRequestCountView;
  @BindView(R.id.debug_okhttp_cache_network_count) TextView okHttpCacheNetworkCountView;
  @BindView(R.id.debug_okhttp_cache_hit_count) TextView okHttpCacheHitCountView;
  @Inject Lazy<OkHttpClient> client;
  @Inject LumberYard lumberYard;
  @Inject Application app;
  boolean isMockMode = P.debugMockModeEnabled.get();
  NetworkBehavior behavior;
  Preference<Integer> networkDelay = P.debugNetworkDelay.rx();
  Preference<Integer> networkFailurePercent = P.debugNetworkFailurePercent.rx();
  Preference<Integer> networkVariancePercent = P.debugNetworkVariancePercent.rx();
  //  @Inject MockGithubService mockGithubService;
  private Preference<Integer> animationSpeed = P.debugAnimationSpeed.rx();
  private Preference<Boolean> pixelGridEnabled = P.debugPixelGridEnabled.rx();
  private Preference<Boolean> pixelRatioEnabled = P.debugPixelRatioEnabled.rx();
  private Preference<Boolean> scalpelEnabled = P.debugScalpelEnabled.rx();
  private Preference<Boolean> scalpelWireframeEnabled = P.debugScalpelWireframeDrawer.rx();

  public DebugView(Context context) {
    this(context, null);
  }

  public DebugView(Context context, AttributeSet attrs) {
    super(context, attrs);
    DaggerDebugView_Component.builder()
        .applicationComponent(CatchUpApplication.component())
        .build()
        .inject(this);

    behavior = NetworkBehavior.create();
    behavior.setDelay(networkDelay.get(), MILLISECONDS);
    behavior.setFailurePercent(networkFailurePercent.get());
    behavior.setVariancePercent(networkVariancePercent.get());

    // Inflate all of the controls and inject them.
    LayoutInflater.from(context)
        .inflate(R.layout.debug_view_content, this);
    ButterKnife.bind(this);

    setupNetworkSection();
    setupMockBehaviorSection();
    setupUserInterfaceSection();
    setupBuildSection();
    setupDeviceSection();
    setupOkHttpCacheSection();
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

  private static String getSizeString(long bytes) {
    String[] units = new String[] { "B", "KB", "MB", "GB" };
    int unit = 0;
    while (bytes >= 1024) {
      bytes /= 1024;
      unit += 1;
    }
    return bytes + units[unit];
  }

  public void onDrawerOpened() {
    refreshOkHttpCacheStats();
  }

  private void setupNetworkSection() {

    final NetworkDelayAdapter delayAdapter = new NetworkDelayAdapter(getContext());
    networkDelayView.setAdapter(delayAdapter);
    networkDelayView.setSelection(NetworkDelayAdapter.getPositionForValue(behavior.delay(
        MILLISECONDS)));

    RxAdapterView.itemSelections(networkDelayView)
        .map(delayAdapter::getItem)
        .filter(item -> item != behavior.delay(MILLISECONDS))
        .subscribe(selected -> {
          Timber.d("Setting network delay to %sms", selected);
          behavior.setDelay(selected, MILLISECONDS);
          networkDelay.set(selected.intValue());
        });

    final NetworkVarianceAdapter varianceAdapter = new NetworkVarianceAdapter(getContext());
    networkVarianceView.setAdapter(varianceAdapter);
    networkVarianceView.setSelection(NetworkVarianceAdapter.getPositionForValue(behavior.variancePercent()));

    RxAdapterView.itemSelections(networkVarianceView)
        .map(varianceAdapter::getItem)
        .filter(item -> item != behavior.variancePercent())
        .subscribe(selected -> {
          Timber.d("Setting network variance to %s%%", selected);
          behavior.setVariancePercent(selected);
          networkVariancePercent.set(selected);
        });

    final NetworkErrorAdapter errorAdapter = new NetworkErrorAdapter(getContext());
    networkErrorView.setAdapter(errorAdapter);
    networkErrorView.setSelection(NetworkErrorAdapter.getPositionForValue(behavior.failurePercent()));

    RxAdapterView.itemSelections(networkErrorView)
        .map(errorAdapter::getItem)
        .filter(item -> item != behavior.failurePercent())
        .subscribe(selected -> {
          Timber.d("Setting network error to %s%%", selected);
          behavior.setFailurePercent(selected);
          networkFailurePercent.set(selected);
        });

    if (!isMockMode) {
      // Disable network controls if we are not in mock mode.
      networkDelayView.setEnabled(false);
      networkVarianceView.setEnabled(false);
      networkErrorView.setEnabled(false);
    }
  }

  private void setupMockBehaviorSection() {
    enableMockModeView.setChecked(P.debugMockModeEnabled.get());
    RxView.clicks(enableMockModeView)
        .subscribe(v -> {
          P.debugMockModeEnabled.put(enableMockModeView.isChecked())
              .apply();
          ProcessPhoenix.triggerRebirth(getContext());
        });
  }

  private void setupUserInterfaceSection() {
    final AnimationSpeedAdapter speedAdapter = new AnimationSpeedAdapter(getContext());
    uiAnimationSpeedView.setAdapter(speedAdapter);
    final int animationSpeedValue = animationSpeed.get();
    uiAnimationSpeedView.setSelection(AnimationSpeedAdapter.getPositionForValue(animationSpeedValue));

    RxAdapterView.itemSelections(uiAnimationSpeedView)
        .map(speedAdapter::getItem)
        .filter(item -> !item.equals(animationSpeed.get()))
        .subscribe(selected -> {
          Timber.d("Setting animation speed to %sx", selected);
          animationSpeed.set(selected);
          applyAnimationSpeed(selected);
        });
    // Ensure the animation speed value is always applied across app restarts.
    post(() -> applyAnimationSpeed(animationSpeedValue));

    boolean gridEnabled = pixelGridEnabled.get();
    uiPixelGridView.setChecked(gridEnabled);
    uiPixelRatioView.setEnabled(gridEnabled);
    uiPixelGridView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel grid overlay enabled to %b", isChecked);
      pixelGridEnabled.set(isChecked);
      uiPixelRatioView.setEnabled(isChecked);
    });

    uiPixelRatioView.setChecked(pixelRatioEnabled.get());
    uiPixelRatioView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting pixel scale overlay enabled to %b", isChecked);
      pixelRatioEnabled.set(isChecked);
    });

    uiScalpelView.setChecked(scalpelEnabled.get());
    uiScalpelWireframeView.setEnabled(scalpelEnabled.get());
    uiScalpelView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel interaction enabled to %b", isChecked);
      scalpelEnabled.set(isChecked);
      uiScalpelWireframeView.setEnabled(isChecked);
    });

    uiScalpelWireframeView.setChecked(scalpelWireframeEnabled.get());
    uiScalpelWireframeView.setOnCheckedChangeListener((buttonView, isChecked) -> {
      Timber.d("Setting scalpel wireframe enabled to %b", isChecked);
      scalpelWireframeEnabled.set(isChecked);
    });
  }

  @OnClick(R.id.debug_logs_show) void showLogs() {
    new LogsDialog(new ContextThemeWrapper(getContext(), R.style.CatchUp), lumberYard).show();
  }

  @OnClick(R.id.debug_leaks_show) void showLeaks() {
    Intent intent = new Intent(getContext(), DisplayLeakActivity.class);
    getContext().startActivity(intent);
  }

  private void setupBuildSection() {
    buildNameView.setText(BuildConfig.VERSION_NAME);
    buildCodeView.setText(String.valueOf(BuildConfig.VERSION_CODE));
    buildShaView.setText(BuildConfig.GIT_SHA);

    TemporalAccessor buildTime = Instant.ofEpochSecond(BuildConfig.GIT_TIMESTAMP);
    buildDateView.setText(DATE_DISPLAY_FORMAT.format(buildTime));
  }

  private void setupDeviceSection() {
    DisplayMetrics displayMetrics = getContext().getResources()
        .getDisplayMetrics();
    String densityBucket = getDensityString(displayMetrics);
    deviceMakeView.setText(Strings.truncateAt(Build.MANUFACTURER, 20));
    deviceModelView.setText(Strings.truncateAt(Build.MODEL, 20));
    deviceResolutionView.setText(displayMetrics.heightPixels + "x" + displayMetrics.widthPixels);
    deviceDensityView.setText(displayMetrics.densityDpi + "dpi (" + densityBucket + ")");
    deviceReleaseView.setText(Build.VERSION.RELEASE);
    deviceApiView.setText(String.valueOf(Build.VERSION.SDK_INT));
  }

  private void setupOkHttpCacheSection() {
    Observable.fromCallable(() -> client.get()
        .cache())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cache -> {
          okHttpCacheMaxSizeView.setText(getSizeString(cache.maxSize()));
          refreshOkHttpCacheStats();
        });
  }

  private void refreshOkHttpCacheStats() {
    Cache cache = client.get()
        .cache(); // Shares the cache with apiClient, so no need to check both.
    int writeTotal = cache.writeSuccessCount() + cache.writeAbortCount();
    int percentage = (int) ((1f * cache.writeAbortCount() / writeTotal) * 100);
    okHttpCacheWriteErrorView.setText(cache.writeAbortCount()
        + " / "
        + writeTotal
        + " ("
        + percentage
        + "%)");
    okHttpCacheRequestCountView.setText(String.valueOf(cache.requestCount()));
    okHttpCacheNetworkCountView.setText(String.valueOf(cache.networkCount()));
    okHttpCacheHitCountView.setText(String.valueOf(cache.hitCount()));
  }

  private void applyAnimationSpeed(int multiplier) {
    try {
      Method method = ValueAnimator.class.getDeclaredMethod("setDurationScale", float.class);
      method.invoke(null, (float) multiplier);
    } catch (Exception e) {
      throw new RuntimeException("Unable to apply animation speed.", e);
    }
  }

  @PerView
  @dagger.Component(dependencies = ApplicationComponent.class)
  public interface Component {
    void inject(DebugView debugView);
  }
}
