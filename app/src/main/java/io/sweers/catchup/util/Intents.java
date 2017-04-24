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

package io.sweers.catchup.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

import io.sweers.catchup.R;

import static android.widget.Toast.LENGTH_LONG;

public final class Intents {
  private Intents() {
    throw new InstantiationError();
  }

  /**
   * Attempt to launch the supplied {@link Intent}. Queries on-device packages before launching and
   * will display a simple message if none are available to handle it.
   */
  public static boolean maybeStartActivity(Context context, Intent intent) {
    return maybeStartActivity(context, intent, false);
  }

  /**
   * Attempt to launch Android's chooser for the supplied {@link Intent}. Queries on-device
   * packages before launching and will display a simple message if none are available to handle
   * it.
   */
  public static boolean maybeStartChooser(Context context, Intent intent) {
    return maybeStartActivity(context, intent, true);
  }

  private static boolean maybeStartActivity(Context context, Intent intent, boolean chooser) {
    if (hasHandler(context, intent)) {
      if (chooser) {
        intent = Intent.createChooser(intent, null);
      }
      context.startActivity(intent);
      return true;
    } else {
      Toast.makeText(context, R.string.no_intent_handler, LENGTH_LONG).show();
      return false;
    }
  }

  /**
   * Queries on-device packages for a handler for the supplied {@link Intent}.
   */
  private static boolean hasHandler(Context context, Intent intent) {
    List<ResolveInfo> handlers = context.getPackageManager().queryIntentActivities(intent, 0);
    return !handlers.isEmpty();
  }
}
