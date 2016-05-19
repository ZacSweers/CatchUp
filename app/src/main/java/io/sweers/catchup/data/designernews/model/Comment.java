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
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Date;
import java.util.List;

/**
 * Models a comment on a designer news story.
 */
@AutoValue
public abstract class Comment {

  public static JsonAdapter<Comment> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Comment.MoshiJsonAdapter(moshi);
  }

  public abstract String body();

  @Json(name = "body_html")
  public abstract String bodyHtml();

  public abstract List<Comment> comments();

  @Json(name = "created_at")
  public abstract Date createdAt();

  public abstract int depth();

  public abstract long id();

  public abstract boolean upvoted();

  @Json(name = "user_display_name")
  public abstract String userDisplayName();

  @Json(name = "user_id")
  public abstract long userId();

  @Json(name = "user_job")
  @Nullable
  public abstract String userJob();

  @Json(name = "user_portrait_url")
  @Nullable
  public abstract String userPortraitUrl();

  @Json(name = "vote_count")
  public abstract int voteCount();

}
