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
import org.threeten.bp.Instant;

/**
 * Models a dribbble user
 */
@AutoValue
public abstract class User {

  public static JsonAdapter<User> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_User.MoshiJsonAdapter(moshi);
  }

  @Json(name = "avatar_url") public abstract String avatarUrl();

  public abstract String bio();

  @Json(name = "buckets_count") public abstract int bucketsCount();

  @Json(name = "buckets_url") public abstract String bucketsUrl();

  @Json(name = "created_at") public abstract Instant createdAt();

  @Json(name = "followers_count") public abstract int followersCount();

  @Json(name = "followers_url") public abstract String followersUrl();

  @Json(name = "following_url") public abstract String followingUrl();

  @Json(name = "followings_count") public abstract int followingsCount();

  @Json(name = "html_url") public abstract String htmlUrl();

  public abstract long id();

  @Json(name = "likes_count") public abstract int likesCount();

  @Json(name = "likes_url") public abstract String likesUrl();

  public abstract Map<String, String> links();

  @Nullable public abstract String location();

  public abstract String name();

  public abstract Boolean pro();

  @Json(name = "projects_count") public abstract int projectsCount();

  @Json(name = "projects_url") public abstract String projectsUrl();

  @Json(name = "shots_count") public abstract int shotsCount();

  @Json(name = "shots_url") public abstract String shotsUrl();

  @Json(name = "teams_count") public abstract int teamsCount();

  @Nullable @Json(name = "teams_url") public abstract String teamsUrl();

  public abstract String type();

  @Json(name = "updated_at") public abstract Instant updatedAt();

  public abstract String username();
}
