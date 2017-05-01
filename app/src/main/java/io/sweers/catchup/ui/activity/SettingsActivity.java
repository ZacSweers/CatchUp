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

package io.sweers.catchup.ui.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.util.DataUtil;
import io.sweers.catchup.util.NumberUtil;

public class SettingsActivity extends AppCompatActivity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
          .add(R.id.container, new SettingsFrag())
          .commit();
    }
  }

  public static class SettingsFrag extends PreferenceFragment {

    public SettingsFrag() {
    }

    @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.prefs_general);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
      switch (preference.getKey()) {
        case P.clearCache.key:
          long cleaned = DataUtil.clearCache(getActivity().getApplicationContext());
          Toast.makeText(
              getActivity(),
              getString(R.string.clear_cache_success, NumberUtil.format(cleaned)),
              Toast.LENGTH_SHORT)
              .show();
          break;
        case P.licenses.key:
          Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT)
              .show();
          break;
        case P.about.key:
          Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT)
              .show();
          break;
      }

      return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
  }
}
