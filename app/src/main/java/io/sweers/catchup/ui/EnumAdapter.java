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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;

public class EnumAdapter<T extends Enum<T>> extends BindableAdapter<T> {
  private final T[] enumConstants;
  private final boolean showNull;
  private final int nullOffset;

  public EnumAdapter(Context context, Class<T> enumType) {
    this(context, enumType, false);
  }

  public EnumAdapter(Context context, Class<T> enumType, boolean showNull) {
    super(context);
    this.enumConstants = enumType.getEnumConstants();
    this.showNull = showNull;
    this.nullOffset = showNull ? 1 : 0;
  }

  @Override
  public final int getCount() {
    return enumConstants.length + nullOffset;
  }

  @Override
  public final T getItem(int position) {
    if (showNull && position == 0) {
      return null;
    }

    return enumConstants[position - nullOffset];
  }

  @Override
  public final long getItemId(int position) {
    return position;
  }

  @Override
  public View newView(LayoutInflater inflater, int position, ViewGroup container) {
    return inflater.inflate(android.R.layout.simple_spinner_item, container, false);
  }

  @Override
  public final void bindView(T item, int position, View view) {
    TextView tv = ButterKnife.findById(view, android.R.id.text1);
    tv.setText(getName(item));
  }

  @Override
  public final View newDropDownView(LayoutInflater inflater, int position, ViewGroup container) {
    return inflater.inflate(android.R.layout.simple_spinner_dropdown_item, container, false);
  }

  protected String getName(T item) {
    return String.valueOf(item);
  }
}
