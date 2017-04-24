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

package io.sweers.catchup.ui.debug;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.sweers.catchup.ui.BindableAdapter;

import static butterknife.ButterKnife.findById;

class AnimationSpeedAdapter extends BindableAdapter<Integer> {
  private static final int[] VALUES = {
      1, 2, 3, 5, 10
  };

  AnimationSpeedAdapter(Context context) {
    super(context);
  }

  public static int getPositionForValue(int value) {
    for (int i = 0; i < VALUES.length; i++) {
      if (VALUES[i] == value) {
        return i;
      }
    }
    return 0; // Default to 1x if something changes.
  }

  @Override
  public int getCount() {
    return VALUES.length;
  }

  @Override
  public Integer getItem(int position) {
    return VALUES[position];
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View newView(LayoutInflater inflater, int position, ViewGroup container) {
    return inflater.inflate(android.R.layout.simple_spinner_item, container, false);
  }

  @Override
  public void bindView(Integer item, int position, View view) {
    TextView tv = findById(view, android.R.id.text1);
    if (item == 1) {
      tv.setText("Normal");
    } else {
      tv.setText(item + "x slower");
    }
  }

  @Override
  public View newDropDownView(LayoutInflater inflater, int position, ViewGroup container) {
    return inflater.inflate(android.R.layout.simple_spinner_dropdown_item, container, false);
  }
}
