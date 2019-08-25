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
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.Lazy
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.DebugPreferences
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.flowbinding.viewScope
import io.sweers.catchup.ui.activity.LauncherActivity
import io.sweers.catchup.ui.logs.LogsDialog
import io.sweers.catchup.util.d
import io.sweers.catchup.util.isN
import io.sweers.catchup.util.kotlin.applyOn
import io.sweers.catchup.util.kotlin.getValue
import io.sweers.catchup.util.kotlin.setValue
import io.sweers.catchup.util.truncateAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotterknife.bindView
import kotterknife.onSubviewClick
import leakcanary.LeakCanary
import okhttp3.OkHttpClient
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.mock.NetworkBehavior
import ru.ldralighieri.corbind.view.clicks
import ru.ldralighieri.corbind.widget.itemSelections
import java.util.Locale
import java.util.concurrent.TimeUnit.MILLISECONDS

// TODO check out jw's assisted injection. Dagger-android doesn't make view injection easy
//  because it doesn't support it, and via subcomponents we can't get ahold of an instance of the
//  internal ActivityComponent
@SuppressLint("SetTextI18n")
class DebugView(
  context: Context,
  attrs: AttributeSet? = null,
  private val client: Lazy<OkHttpClient>,
  private val lumberYard: LumberYard,
  private val debugPreferences: DebugPreferences
) : FrameLayout(context, attrs) {
  internal val icon by bindView<View>(R.id.debug_icon)
  private val networkDelayView by bindView<Spinner>(R.id.debug_network_delay)
  private val networkVarianceView by bindView<Spinner>(R.id.debug_network_variance)
  private val networkErrorView by bindView<Spinner>(R.id.debug_network_error)
  private val enableMockModeView by bindView<Switch>(R.id.debug_enable_mock_mode)
  private val uiAnimationSpeedView by bindView<Spinner>(R.id.debug_ui_animation_speed)
  private val uiPixelGridView by bindView<Switch>(R.id.debug_ui_pixel_grid)
  private val uiPixelRatioView by bindView<Switch>(R.id.debug_ui_pixel_ratio)
  private val uiScalpelView by bindView<Switch>(R.id.debug_ui_scalpel)
  private val uiScalpelWireframeView by bindView<Switch>(R.id.debug_ui_scalpel_wireframe)
  private val buildNameView by bindView<TextView>(R.id.debug_build_name)
  private val buildCodeView by bindView<TextView>(R.id.debug_build_code)
  private val buildShaView by bindView<TextView>(R.id.debug_build_sha)
  private val buildDateView by bindView<TextView>(R.id.debug_build_date)
  private val deviceMakeView by bindView<TextView>(R.id.debug_device_make)
  private val deviceModelView by bindView<TextView>(R.id.debug_device_model)
  private val deviceResolutionView by bindView<TextView>(R.id.debug_device_resolution)
  private val deviceDensityView by bindView<TextView>(R.id.debug_device_density)
  private val deviceReleaseView by bindView<TextView>(R.id.debug_device_release)
  private val deviceApiView by bindView<TextView>(R.id.debug_device_api)
  private val okHttpCacheMaxSizeView by bindView<TextView>(R.id.debug_okhttp_cache_max_size)
  private val okHttpCacheWriteErrorView by bindView<TextView>(R.id.debug_okhttp_cache_write_error)
  private val okHttpCacheRequestCountView by bindView<TextView>(
      R.id.debug_okhttp_cache_request_count)
  private val okHttpCacheNetworkCountView by bindView<TextView>(
      R.id.debug_okhttp_cache_network_count)
  private val okHttpCacheHitCountView by bindView<TextView>(R.id.debug_okhttp_cache_hit_count)
  private var isMockMode by debugPreferences::mockModeEnabled
  private var behavior: NetworkBehavior = NetworkBehavior.create().apply {
    setDelay(debugPreferences.networkDelay, MILLISECONDS)
    setFailurePercent(debugPreferences.networkFailurePercent)
    setVariancePercent(debugPreferences.networkVariancePercent)
  }
  private var animationSpeed by debugPreferences::animationSpeed
  private var pixelGridEnabled by debugPreferences::pixelGridEnabled
  private var pixelRatioEnabled by debugPreferences::pixelRatioEnabled
  private var scalpelEnabled by debugPreferences::scalpelEnabled
  private var scalpelWireframeEnabled by debugPreferences::scalpelWireframeDrawer

  init {
    // Inflate all of the controls and inject them.
    LayoutInflater.from(context)
        .inflate(R.layout.debug_view_content, this)

    onSubviewClick<View>(R.id.debug_logs_show) {
      LogsDialog(ContextThemeWrapper(context, R.style.CatchUp), lumberYard).show()
    }
    onSubviewClick<View>(R.id.debug_leaks_show) {
      startDebugActivity(LeakCanary.newLeakDisplayActivityIntent())
    }
//    onSubviewClick<View>(R.id.debug_network_logs) {
//      startDebugActivity(Intent(context, MainActivity::class.java))
//    }

    setupNetworkSection()
    setupMockBehaviorSection()
    setupUserInterfaceSection()
    setupBuildSection()
    setupDeviceSection()
    viewScope().launch { refreshOkHttpCacheStats(setup = true) }
  }

  fun onDrawerOpened() {
    viewScope().launch { refreshOkHttpCacheStats(setup = false) }
  }

  private fun setupNetworkSection() {

    val delayAdapter = NetworkDelayAdapter(context)
    networkDelayView.adapter = delayAdapter
    networkDelayView.setSelection(NetworkDelayAdapter.getPositionForValue(behavior.delay(
        MILLISECONDS)))

    viewScope().launch {
      networkDelayView.itemSelections()
          .map { delayAdapter.getItem(it) }
          .filter { item -> item != behavior.delay(MILLISECONDS) }
          .collect { selected ->
            d { "Setting network delay to ${selected}ms" }
            behavior.setDelay(selected, MILLISECONDS)
            debugPreferences.networkDelay = selected
          }
    }

    val varianceAdapter = NetworkVarianceAdapter(context)
    networkVarianceView.adapter = varianceAdapter
    networkVarianceView.setSelection(
        NetworkVarianceAdapter.getPositionForValue(behavior.variancePercent()))

    viewScope().launch {
      networkVarianceView.itemSelections()
          .map { varianceAdapter.getItem(it) }
          .filter { item -> item != behavior.variancePercent() }
          .collect { selected ->
            d { "Setting network variance to $selected%" }
            behavior.setVariancePercent(selected)
            debugPreferences.networkVariancePercent = selected
          }
    }

    val errorAdapter = NetworkErrorAdapter(context)
    networkErrorView.adapter = errorAdapter
    networkErrorView.setSelection(
        NetworkErrorAdapter.getPositionForValue(behavior.failurePercent()))

    viewScope().launch {
      networkErrorView.itemSelections()
          .map { errorAdapter.getItem(it) }
          .filter { item -> item != behavior.failurePercent() }
          .collect { selected ->
            d { "Setting network error to $selected%" }
            behavior.setFailurePercent(selected)
            debugPreferences.networkFailurePercent = selected
          }
    }

    if (!isMockMode) {
      // Disable network controls if we are not in mock mode.
      applyOn(networkDelayView, networkVarianceView, networkErrorView) {
        isEnabled = false
      }
    }
  }

  private fun setupMockBehaviorSection() {
    enableMockModeView.isChecked = debugPreferences.mockModeEnabled
    viewScope().launch {
      enableMockModeView.clicks()
          .collect {
            debugPreferences.mockModeEnabled = enableMockModeView.isChecked
            ProcessPhoenix.triggerRebirth(context, Intent(context, LauncherActivity::class.java))
          }
    }
  }

  private fun setupUserInterfaceSection() {
    val speedAdapter = AnimationSpeedAdapter(context)
    uiAnimationSpeedView.adapter = speedAdapter
    val animationSpeedValue = animationSpeed
    uiAnimationSpeedView.setSelection(
        AnimationSpeedAdapter.getPositionForValue(animationSpeedValue))

    viewScope().launch {
      uiAnimationSpeedView.itemSelections()
          .map { speedAdapter.getItem(it) }
          .filter { item -> item != animationSpeed }
          .collect { selected ->
            d { "Setting animation speed to ${selected}x" }
            animationSpeed = selected
            applyAnimationSpeed(selected)
          }
    }
    // Ensure the animation speed value is always applied across app restarts.
    post { applyAnimationSpeed(animationSpeedValue) }

    val gridEnabled = pixelGridEnabled
    uiPixelGridView.isChecked = gridEnabled
    uiPixelRatioView.isEnabled = gridEnabled
    uiPixelGridView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting pixel grid overlay enabled to $isChecked" }
      pixelGridEnabled = isChecked
      uiPixelRatioView.isEnabled = isChecked
    }

    uiPixelRatioView.isChecked = pixelRatioEnabled
    uiPixelRatioView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting pixel scale overlay enabled to $isChecked" }
      pixelRatioEnabled = isChecked
    }

    uiScalpelView.isChecked = scalpelEnabled
    uiScalpelWireframeView.isEnabled = scalpelEnabled
    uiScalpelView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting scalpel interaction enabled to $isChecked" }
      scalpelEnabled = isChecked
      uiScalpelWireframeView.isEnabled = isChecked
    }

    uiScalpelWireframeView.isChecked = scalpelWireframeEnabled
    uiScalpelWireframeView.setOnCheckedChangeListener { _, isChecked ->
      d { "Setting scalpel wireframe enabled to $isChecked" }
      scalpelWireframeEnabled = isChecked
    }
  }

  @SuppressLint("InlinedApi") // False positive
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
    buildShaView.text = buildNameView.resources.getString(R.string.git_sha)

    val buildTime = Instant.ofEpochSecond(
        buildNameView.resources.getInteger(R.integer.git_timestamp).toLong())
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

  private suspend fun refreshOkHttpCacheStats(setup: Boolean = false) {
    val cache = withContext(Dispatchers.IO) { client.get().cache } ?: return
    if (setup) {
      okHttpCacheMaxSizeView.text = getSizeString(cache.maxSize())
    }
    val writeTotal = cache.writeSuccessCount() + cache.writeAbortCount()
    val percentage = (1f * cache.writeAbortCount() / writeTotal * 100).toInt()
    okHttpCacheWriteErrorView.text = "${cache.writeAbortCount()} / $writeTotal ($percentage%)"
    okHttpCacheRequestCountView.text = cache.requestCount().toString()
    okHttpCacheNetworkCountView.text = cache.networkCount().toString()
    okHttpCacheHitCountView.text = cache.hitCount().toString()
  }

  @SuppressLint("DiscouragedPrivateApi")
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
      return when (val dpi = displayMetrics.densityDpi) {
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
