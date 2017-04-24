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

package io.sweers.catchup.ui.widget;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * An extension to image view that has a circular outline.
 */
public class CircularImageView extends ForegroundImageView {

  public static final ViewOutlineProvider CIRCULAR_OUTLINE = new ViewOutlineProvider() {
    @Override
    public void getOutline(View view, Outline outline) {
      outline.setOval(view.getPaddingLeft(),
          view.getPaddingTop(),
          view.getWidth() - view.getPaddingRight(),
          view.getHeight() - view.getPaddingBottom());
    }
  };

  public CircularImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOutlineProvider(CIRCULAR_OUTLINE);
    setClipToOutline(true);
  }
}
