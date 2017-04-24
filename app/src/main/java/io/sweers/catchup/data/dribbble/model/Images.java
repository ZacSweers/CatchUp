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

package io.sweers.catchup.data.dribbble.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Models links to the various quality of images of a shot.
 */
@AutoValue
public abstract class Images {

  private static final int[] NORMAL_IMAGE_SIZE = new int[]{400, 300};
  private static final int[] TWO_X_IMAGE_SIZE = new int[]{800, 600};

  public static JsonAdapter<Images> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Images.MoshiJsonAdapter(moshi);
  }

  @Nullable
  public abstract String hidpi();

  public abstract String normal();

  public abstract String teaser();


  public String best() {
    return !TextUtils.isEmpty(hidpi()) ? hidpi() : normal();
  }

  public int[] bestSize() {
    return !TextUtils.isEmpty(hidpi()) ? TWO_X_IMAGE_SIZE : NORMAL_IMAGE_SIZE;
  }

}
