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

package io.sweers.catchup.ui.debug

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.Switch
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.jakewharton.processphoenix.ProcessPhoenix
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxAdapterView
import com.readystatesoftware.chuck.internal.ui.MainActivity
import com.squareup.leakcanary.internal.DisplayLeakActivity
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.ui.logs.LogsDialog
import io.sweers.catchup.util.applyOn
import io.sweers.catchup.util.d
import io.sweers.catchup.util.isN
import io.sweers.catchup.util.truncateAt
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.mock.NetworkBehavior
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS

@SuppressLint("SetTextI18n")
class DebugView @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
  @BindView(R.id.debug_icon) lateinit var icon: View
  @BindView(R.id.debug_contextual_title) lateinit var contextualTitleView: View
  @BindView(R.id.debug_contextual_list) lateinit var contextualListView: LinearLayout
  @BindView(R.id.debug_network_delay) lateinit var networkDelayView: Spinner
  @BindView(R.id.debug_network_variance) lateinit var networkVarianceView: Spinner
  @BindView(R.id.debug_network_error) lateinit var networkErrorView: Spinner
  @BindView(R.id.debug_enable_mock_mode) lateinit var enableMockModeView: Switch
  @BindView(R.id.debug_ui_animation_speed) lateinit var uiAnimationSpeedView: Spinner
  @BindView(R.id.debug_ui_pixel_grid) lateinit var uiPixelGridView: Switch
  @BindView(R.id.debug_ui_pixel_ratio) lateinit var uiPixelRatioView: Switch
  @BindView(R.id.debug_ui_scalpel) lateinit var uiScalpelView: Switch
  @BindView(R.id.debug_ui_scalpel_wireframe) lateinit var uiScalpelWireframeView: Switch
  @BindView(R.id.debug_build_name) lateinit var buildNameView: TextView
  @BindView(R.id.debug_build_code) lateinit var buildCodeView: TextView
  @BindView(R.id.debug_build_sha) lateinit var buildShaView: TextView
  @BindView(R.id.debug_build_date) lateinit var buildDateView: TextView
  @BindView(R.id.debug_device_make) lateinit var deviceMakeView: TextView
  @BindView(R.id.debug_device_model) lateinit var deviceModelView: TextView
  @BindView(R.id.debug_device_resolution) lateinit var deviceResolutionView: TextView
  @BindView(R.id.debug_device_density) lateinit var deviceDensityView: TextView
  @BindView(R.id.debug_device_release) lateinit var deviceReleaseView: TextView
  @BindView(R.id.debug_device_api) lateinit var deviceApiView: TextView
  @BindView(R.id.debug_okhttp_cache_max_size) lateinit var okHttpCacheMaxSizeView: TextView
  @BindView(R.id.debug_okhttp_cache_write_error) lateinit var okHttpCacheWriteErrorView: TextView
  @BindView(R.id.debug_okhttp_cache_request_count)
  lateinit var okHttpCacheRequestCountView: TextView
  @BindView(R.id.debug_okhttp_cache_network_count)
  lateinit var okHttpCacheNetworkCountView: TextView
  @BindView(R.id.debug_okhttp_cache_hit_count) lateinit var okHttpCacheHitCountView: TextView
  private lateinit var client: Lazy<OkHttpClient>
  private lateinit var lumberYard: LumberYard
  private var isMockMode = P.DebugMockModeEnabled.get()
  private var networkDelay = P.DebugNetworkDelay.rx()
  private var networkFailurePercent = P.DebugNetworkFailurePercent.rx()
  private var networkVariancePercent = P.DebugNetworkVariancePercent.rx()
  private var behavior: NetworkBehavior = NetworkBehavior.create().apply {
    setDelay(networkDelay.get().toLong(), MILLISECONDS)
    setFailurePercent(networkFailurePercent.get())
    setVariancePercent(networkVariancePercent.get())
  }
  private val animationSpeed = P.DebugAnimationSpeed.rx()
  private val pixelGridEnabled = P.DebugPixelGridEnabled.rx()
  private val pixelRatioEnabled = P.DebugPixelRatioEnabled.rx()
  private val scalpelEnabled = P.DebugScalpelEnabled.rx()
  private val scalpelWireframeEnabled = P.DebugScalpelWireframeDrawer.rx()

  constructor(context: Context,
      client: Lazy<OkHttpClient>,
      lumberYard: LumberYard) : this(context) {
    // TODO check out jw's assisted injection. Dagger-android doesn't make view injection easy
    // because it doesn't support it, and via subcomponents we can't get ahold of an instance of the
    // internal ActivityComponent
    this.client = client
    this.lumberYard = lumberYard
    realInit()
  }

  // A little scary but it's not safe to just use init {} here because it sometimes gets called in
  // the super constructor call before our actual constructor finishes.
  private fun realInit() {
    // Inflate all of the controls and inject them.
    LayoutInflater.from(context)
        .inflate(R.layout.debug_view_content, this)
    DebugView_ViewBinding(this)

    setupNetworkSection()
    setupMockBehaviorSection()
    setupUserInterfaceSection()
    setupBuildSection()
    setupDeviceSection()
    setupOkHttpCacheSection()
  }

  fun onDrawerOpened() {
    refreshOkHttpCacheStats()
  }

  private fun setupNetworkSection() {

    val delayAdapter = NetworkDelayAdapter(context)
    networkDelayView.adapter = delayAdapter
    networkDelayView.setSelection(NetworkDelayAdapter.getPositionForValue(behavior.delay(
        MILLISECONDS)))

    RxAdapterView.itemSelections<SpinnerAdapter>(networkDelayView)
        .map { delayAdapter.getItem(it) }
        .filter { item -> item != behavior.delay(MILLISECONDS) }
        .subscribe { selected ->
          d { "Setting network delay to ${selected}ms" }
          behavior.setDelay(selected, MILLISECONDS)
          networkDelay.set(selected.toInt())
        }

    val varianceAdapter = NetworkVarianceAdapter(context)
    networkVarianceView.adapter = varianceAdapter
    networkVarianceView.setSelection(
        NetworkVarianceAdapter.getPositionForValue(behavior.variancePercent()))

    RxAdapterView.itemSelections<SpinnerAdapter>(networkVarianceView)
        .map { varianceAdapter.getItem(it) }
        .filter { item -> item != behavior.variancePercent() }
        .subscribe { selected ->
          d { "Setting network variance to $selected%" }
          behavior.setVariancePercent(selected)
          networkVariancePercent.set(selected)
        }

    val errorAdapter = NetworkErrorAdapter(context)
    networkErrorView.adapter = errorAdapter
    networkErrorView.setSelection(
        NetworkErrorAdapter.getPositionForValue(behavior.failurePercent()))

    RxAdapterView.itemSelections<SpinnerAdapter>(networkErrorView)
        .map { errorAdapter.getItem(it) }
        .filter { item -> item != behavior.failurePercent() }
        .subscribe { selected ->
          d { "Setting network error to $selected%" }
          behavior.setFailurePercent(selected)
          networkFailurePercent.set(selected)
        }

    if (!isMockMode) {
      // Disable network controls if we are not in mock mode.
      applyOn(networkDelayView, networkVarianceView, networkErrorView) {
        isEnabled = false
      }
    }
  }

  private fun setupMockBehaviorSection() {
    enableMockModeView.isChecked = P.DebugMockModeEnabled.get()
    RxView.clicks(enableMockModeView)
        .subscribe {
          P.DebugMockModeEnabled.put(enableMockModeView.isChecked).commit()
          ProcessPhoenix.triggerRebirth(context)
        }
  }

  private fun setupUserInterfaceSection() {
    val speedAdapter = AnimationSpeedAdapter(context)
    uiAnimationSpeedView.adapter = speedAdapter
    val animationSpeedValue = animationSpeed.get()
    uiAnimationSpeedView.setSelection(
        AnimationSpeedAdapter.getPositionForValue(animationSpeedValue))

    RxAdapterView.itemSelections<SpinnerAdapter>(uiAnimationSpeedView)
        .map { speedAdapter.getItem(it) }
        .filter { item -> item != animationSpeed.get() }
        .subscribe { selected ->
          d { "Setting animation speed to ${selected}x" }
          animationSpeed.set(selected)
          applyAnimationSpeed(selected)
        }
    // Ensure the animation speed value is always applied across app restarts.
    post { applyAnimationSpeed(animationSpeedValue) }

    val gridEnabled = pixelGridEnabled.get()
    uiPixelGridView.isChecked = gridEnabled
    uiPixelRatioView.isEnabled = gridEnabled
    uiPixelGridView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting pixel grid overlay enabled to $isChecked" }
      pixelGridEnabled.set(isChecked)
      uiPixelRatioView.isEnabled = isChecked
    }

    uiPixelRatioView.isChecked = pixelRatioEnabled.get()
    uiPixelRatioView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting pixel scale overlay enabled to $isChecked" }
      pixelRatioEnabled.set(isChecked)
    }

    uiScalpelView.isChecked = scalpelEnabled.get()
    uiScalpelWireframeView.isEnabled = scalpelEnabled.get()
    uiScalpelView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting scalpel interaction enabled to $isChecked" }
      scalpelEnabled.set(isChecked)
      uiScalpelWireframeView.isEnabled = isChecked
    }

    uiScalpelWireframeView.isChecked = scalpelWireframeEnabled.get()
    uiScalpelWireframeView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting scalpel wireframe enabled to $isChecked" }
      scalpelWireframeEnabled.set(isChecked)
    }
  }

  @OnClick(R.id.debug_logs_show) internal fun showLogs() {
    LogsDialog(ContextThemeWrapper(context, R.style.CatchUp), lumberYard).show()
  }

  @OnClick(R.id.debug_leaks_show) internal fun showLeaks() {
    startDebugActivity(Intent(context, DisplayLeakActivity::class.java))
  }

  @OnClick(R.id.debug_network_logs) internal fun showNetworkLogs() {
    startDebugActivity(Intent(context, MainActivity::class.java))
  }

  private fun startDebugActivity(intent: Intent) {
    if (isN()) {
      // In case they're for some reason already in multiwindow
      // annoying that we can't request that an app go to multiwindow if not in it already :/
      intent.flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
    }
    context.startActivity(intent)
  }

  private fun setupBuildSection() {
    buildNameView.text = BuildConfig.VERSION_NAME
    buildCodeView.text = BuildConfig.VERSION_CODE.toString()
    buildShaView.text = BuildConfig.GIT_SHA

    val buildTime = Instant.ofEpochSecond(BuildConfig.GIT_TIMESTAMP)
    buildDateView.text = DATE_DISPLAY_FORMAT.format(buildTime)
  }

  private fun setupDeviceSection() {
    val displayMetrics = context.resources
        .displayMetrics
    val densityBucket = getDensityString(displayMetrics)
    deviceMakeView.text = Build.MANUFACTURER truncateAt 20
    deviceModelView.text = Build.MODEL truncateAt 20
    deviceResolutionView.text = "${displayMetrics.heightPixels}x${displayMetrics.widthPixels}"
    deviceDensityView.text = "${displayMetrics.densityDpi}dpi ($densityBucket)"
    deviceReleaseView.text = Build.VERSION.RELEASE
    deviceApiView.text = Build.VERSION.SDK_INT.toString()
  }

  private fun setupOkHttpCacheSection() {
    Observable
        .fromCallable { client.get().cache() }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { cache ->
          okHttpCacheMaxSizeView.text = getSizeString(cache.maxSize())
          refreshOkHttpCacheStats()
        }
  }

  private fun refreshOkHttpCacheStats() {
    val cache = client.get()
        .cache() // Shares the cache with apiClient, so no need to check both.
    val writeTotal = cache.writeSuccessCount() + cache.writeAbortCount()
    val percentage = (1f * cache.writeAbortCount() / writeTotal * 100).toInt()
    okHttpCacheWriteErrorView.text = "${cache.writeAbortCount()} / $writeTotal ($percentage%)"
    okHttpCacheRequestCountView.text = cache.requestCount().toString()
    okHttpCacheNetworkCountView.text = cache.networkCount().toString()
    okHttpCacheHitCountView.text = cache.hitCount().toString()
  }

  private fun applyAnimationSpeed(multiplier: Int) {
    try {
      val method = ValueAnimator::class.java.getDeclaredMethod("setDurationScale",
          Float::class.javaPrimitiveType)
      method.invoke(null, multiplier.toFloat())
    } catch (e: Exception) {
      throw RuntimeException("Unable to apply animation speed.", e)
    }
  }

  companion object {
    private val DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US)
        .withZone(ZoneId.systemDefault())

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

    private fun getSizeString(inputBytes: Long): String {
      var bytes = inputBytes
      val units = arrayOf("B", "KB", "MB", "GB")
      var unit = 0
      while (bytes >= 1024) {
        bytes /= 1024
        unit += 1
      }
      return bytes.toString() + units[unit]
    }
  }
}
