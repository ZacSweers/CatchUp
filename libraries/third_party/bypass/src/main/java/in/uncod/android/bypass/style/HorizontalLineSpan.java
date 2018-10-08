/*
 * Copyright (c) 2018 Zac Sweers
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

package in.uncod.android.bypass.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

/**
 * Draws a line across the screen.
 */
public class HorizontalLineSpan extends ReplacementSpan {

  private Paint mPaint;
  private int mLineHeight;
  private int mTopBottomPadding;

  public HorizontalLineSpan(int color, int lineHeight, int topBottomPadding) {
    mPaint = new Paint();
    mPaint.setColor(color);
    mLineHeight = lineHeight;
    mTopBottomPadding = topBottomPadding;
  }

  @Override
  public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
    if (fm != null) {
      fm.ascent = -mLineHeight - mTopBottomPadding;
      fm.descent = 0;

      fm.top = fm.ascent;
      fm.bottom = 0;
    }

    // Take up *all* the horizontal space
    return Integer.MAX_VALUE;
  }

  @Override public void draw(Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      Paint paint) {
    int middle = (top + bottom) / 2;
    int halfLineHeight = mLineHeight / 2;
    canvas.drawRect(x, middle - halfLineHeight, Integer.MAX_VALUE, middle + halfLineHeight, mPaint);
  }
}
