/*
 * Copyright (c) 2019 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

/**
 * Copied from generated P as a transition holdover
 */
public final class P {
  private static Resources RESOURCES = null;

  private static SharedPreferences PREFERENCES = null;

  private static RxSharedPreferences RX_PREFERENCES = null;

  private P() {
    throw new AssertionError("No instances.");
  }

  /**
   * Initializer that takes a {@link Context} for resource resolution. This will retrieve default shared preferences.
   */
  public static final void init(final Context context) {
    init(context, true);
  }

  /**
   * Initializer that takes a {@link Context} for resource resolution. This will retrieve default shared preferences if {@code autoCreateSharedPrefs} is true.
   */
  public static final void init(final Context context, final boolean autoCreateSharedPrefs) {
    if (context == null) {
      throw new IllegalStateException("context cannot be null!");
    }
    Context applicationContext = context.getApplicationContext();
    RESOURCES = applicationContext.getResources();
    if (autoCreateSharedPrefs) {
      // Sensible default;
      setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(applicationContext));
    }
  }

  public static final void setSharedPreferences(final SharedPreferences sharedPreferences) {
    setSharedPreferences(sharedPreferences, null);
  }

  public static final void setSharedPreferences(final SharedPreferences sharedPreferences,
      @Nullable RxSharedPreferences rxPreferences) {
    if (sharedPreferences == null) {
      throw new IllegalStateException("sharedPreferences cannot be null!");
    }
    PREFERENCES = sharedPreferences;
    if (rxPreferences == null) {
      RX_PREFERENCES = RxSharedPreferences.create(PREFERENCES);
    } else {
      RX_PREFERENCES = rxPreferences;
    }
  }

  public static final class About {
    public static final String KEY = "about";
  }

  public static final class ClearCache {
    public static final String KEY = "clear_cache";
  }

  public static final class DaynightAuto {
    public static final String KEY = "daynight_auto";

    public static final boolean defaultValue() {
      return true;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DaynightNight {
    public static final String KEY = "daynight_night";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugAnimationSpeed {
    public static final String KEY = "debug_animation_speed";

    public static final int defaultValue() {
      return 1;
    }

    public static final int get() {
      return PREFERENCES.getInt(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final int val) {
      return PREFERENCES.edit().putInt(KEY, val);
    }

    @CheckResult
    public static final Preference<Integer> rx() {
      return RX_PREFERENCES.getInteger(KEY, defaultValue());
    }
  }

  public static final class DebugCaptureIntents {
    public static final String KEY = "debug_capture_intents";

    public static final boolean defaultValue() {
      return true;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugMockModeEnabled {
    public static final String KEY = "debug_mock_mode_enabled";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugNetworkDelay {
    public static final String KEY = "debug_network_delay";

    public static final int defaultValue() {
      return 2000;
    }

    public static final int get() {
      return PREFERENCES.getInt(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final int val) {
      return PREFERENCES.edit().putInt(KEY, val);
    }

    @CheckResult
    public static final Preference<Integer> rx() {
      return RX_PREFERENCES.getInteger(KEY, defaultValue());
    }
  }

  public static final class DebugNetworkFailurePercent {
    public static final String KEY = "debug_network_failure_percent";

    public static final int defaultValue() {
      return 3;
    }

    public static final int get() {
      return PREFERENCES.getInt(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final int val) {
      return PREFERENCES.edit().putInt(KEY, val);
    }

    @CheckResult
    public static final Preference<Integer> rx() {
      return RX_PREFERENCES.getInteger(KEY, defaultValue());
    }
  }

  public static final class DebugNetworkVariancePercent {
    public static final String KEY = "debug_network_variance_percent";

    public static final int defaultValue() {
      return 40;
    }

    public static final int get() {
      return PREFERENCES.getInt(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final int val) {
      return PREFERENCES.edit().putInt(KEY, val);
    }

    @CheckResult
    public static final Preference<Integer> rx() {
      return RX_PREFERENCES.getInteger(KEY, defaultValue());
    }
  }

  public static final class DebugPixelGridEnabled {
    public static final String KEY = "debug_pixel_grid_enabled";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugPixelRatioEnabled {
    public static final String KEY = "debug_pixel_ratio_enabled";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugScalpelEnabled {
    public static final String KEY = "debug_scalpel_enabled";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugScalpelWireframeDrawer {
    public static final String KEY = "debug_scalpel_wireframe_drawer";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class DebugSeenDebugDrawer {
    public static final String KEY = "debug_seen_debug_drawer";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class ReorderServicesSection {
    public static final String KEY = "reorder_services_section";
  }

  public static final class Reports {
    public static final String KEY = "reports";

    public static final boolean defaultValue() {
      return true;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class Services {
    public static final String KEY = "services";
  }

  public static final class ServicesOrder {
    public static final String KEY = "services_order";
  }

  public static final class ServicesOrderSeen {
    public static final String KEY = "services_order_seen";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class SmartlinkingGlobal {
    public static final String KEY = "smartlinking_global";

    public static final boolean defaultValue() {
      return true;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }

  public static final class ThemeNavigationBar {
    public static final String KEY = "theme_navigation_bar";

    public static final boolean defaultValue() {
      return false;
    }

    public static final boolean get() {
      return PREFERENCES.getBoolean(KEY, defaultValue());
    }

    @CheckResult
    public static final SharedPreferences.Editor put(final boolean val) {
      return PREFERENCES.edit().putBoolean(KEY, val);
    }

    @CheckResult
    public static final Preference<Boolean> rx() {
      return RX_PREFERENCES.getBoolean(KEY, defaultValue());
    }
  }
}
