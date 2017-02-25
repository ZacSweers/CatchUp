/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import io.sweers.barber.Barber;
import io.sweers.barber.StyledAttr;
import io.sweers.catchup.R;

/**
 * An extension to {@link ImageView} which has a foreground drawable.
 */
public class ForegroundImageView extends AppCompatImageView {

  private Drawable foreground;

  public ForegroundImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    Barber.style(this, attrs, R.styleable.ForegroundView);
    setOutlineProvider(ViewOutlineProvider.BOUNDS);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (foreground != null) {
      foreground.setBounds(0, 0, w, h);
    }
  }

  @Override public boolean hasOverlappingRendering() {
    return false;
  }

  @Override protected boolean verifyDrawable(@NonNull Drawable who) {
    return super.verifyDrawable(who) || (who == foreground);
  }

  @Override public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();
    if (foreground != null) foreground.jumpToCurrentState();
  }

  @Override protected void drawableStateChanged() {
    super.drawableStateChanged();
    if (foreground != null && foreground.isStateful()) {
      foreground.setState(getDrawableState());
    }
  }

  /**
   * Returns the drawable used as the foreground of this view. The
   * foreground drawable, if non-null, is always drawn on top of the children.
   *
   * @return A Drawable or null if no foreground was set.
   */
  public Drawable getForeground() {
    return foreground;
  }

  /**
   * Supply a Drawable that is to be rendered on top of the contents of this ImageView
   *
   * @param drawable The Drawable to be drawn on top of the ImageView
   */
  @SuppressLint("NewApi") @StyledAttr(R.styleable.ForegroundView_android_foreground)
  public void setForeground(@Nullable Drawable drawable) {
    if (foreground != drawable) {
      if (foreground != null) {
        foreground.setCallback(null);
        unscheduleDrawable(foreground);
      }

      foreground = drawable;

      if (foreground != null) {
        foreground.setBounds(0, 0, getWidth(), getHeight());
        setWillNotDraw(false);
        foreground.setCallback(this);
        if (foreground.isStateful()) {
          foreground.setState(getDrawableState());
        }
      } else {
        setWillNotDraw(true);
      }
      invalidate();
    }
  }

  @Override public void draw(Canvas canvas) {
    super.draw(canvas);
    if (foreground != null) {
      foreground.draw(canvas);
    }
  }

  @Override public void drawableHotspotChanged(float x, float y) {
    super.drawableHotspotChanged(x, y);
    if (foreground != null) {
      foreground.setHotspot(x, y);
    }
  }
}
