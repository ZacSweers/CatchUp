/*
 * Copyright (C) 2019. Uber Technologies
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
package in.uncod.android.bypass.style;

import android.content.res.ColorStateList;
import android.text.TextPaint;
import android.text.style.URLSpan;

/**
 * An extension to URLSpan which changes it's background & foreground color when clicked.
 *
 * <p>Derived from http://stackoverflow.com/a/20905824
 */
public class TouchableUrlSpan extends URLSpan {

  private static int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};
  private boolean isPressed;
  private int normalTextColor;
  private int pressedTextColor;
  private int pressedBackgroundColor;

  public TouchableUrlSpan(String url, ColorStateList textColor, int pressedBackgroundColor) {
    super(url);
    this.normalTextColor = textColor.getDefaultColor();
    this.pressedTextColor = textColor.getColorForState(STATE_PRESSED, normalTextColor);
    this.pressedBackgroundColor = pressedBackgroundColor;
  }

  public void setPressed(boolean isPressed) {
    this.isPressed = isPressed;
  }

  @Override
  public void updateDrawState(TextPaint drawState) {
    drawState.setColor(isPressed ? pressedTextColor : normalTextColor);
    drawState.bgColor = isPressed ? pressedBackgroundColor : 0;
    drawState.setUnderlineText(!isPressed);
  }
}
