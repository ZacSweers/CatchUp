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

package io.sweers.catchup.data.dribbble.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Date;

/**
 * Models a commend on a Dribbble shot.
 */
@AutoValue
public abstract class Comment {

  public static JsonAdapter<Comment> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Comment.MoshiJsonAdapter(moshi);
  }

  public abstract String body();

  @Json(name = "created_at")
  public abstract Date createdAt();

  public abstract long id();

  @Json(name = "likes_count")
  public abstract long likesCount();

  @Json(name = "likes_url")
  public abstract String likesUrl();

  @Json(name = "updated_at")
  public abstract Date updatedAt();

  public abstract User user();
}
