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
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Map;

/**
 * Models a Dribbble team.
 */
@AutoValue
public abstract class Team {

  public static JsonAdapter<Team> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Team.MoshiJsonAdapter(moshi);
  }

  @Json(name = "avatar_url")
  public abstract String avatarUrl();

  public abstract String bio();

  @Json(name = "html_url")
  public abstract String htmlUrl();

  public abstract long id();

  public abstract Map<String, String> links();

  @Nullable
  public abstract String location();

  public abstract String name();

  public abstract String username();

}
