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

package io.sweers.catchup.data.designernews.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Models a Desinger News User
 */
@AutoValue
public abstract class User {

  public static JsonAdapter<User> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_User.MoshiJsonAdapter(moshi);
  }

  @Json(name = "cover_photo_url")
  public abstract String coverPhotoUrl();

  @Json(name = "display_name")
  public abstract String displayName();

  @Json(name = "first_name")
  public abstract String firstName();

  public abstract long id();

  public abstract String job();

  @Json(name = "last_name")
  public abstract String lastName();

  @Json(name = "portrait_url")
  public abstract String portraitUrl();

}
