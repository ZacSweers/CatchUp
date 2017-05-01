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
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import java.io.File;
import timber.log.Timber;

public final class DataUtil {

  public static long clearCache(@ApplicationContext Context context) {
    return cleanDir(context.getApplicationContext().getCacheDir());
  }

  private static long cleanDir(File dir) {
    long bytesDeleted = 0;
    File[] files = dir.listFiles();

    for (File file : files) {
      long length = file.length();
      if (file.delete()) {
        Timber.d("Deleted file");
        bytesDeleted += length;
      }
    }
    return bytesDeleted;
  }
}
