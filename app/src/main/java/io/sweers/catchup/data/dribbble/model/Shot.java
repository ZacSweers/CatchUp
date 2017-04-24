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
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.sweers.catchup.ui.base.HasStableId;
import java.util.List;
import org.threeten.bp.Instant;

/**
 * Models a dibbble shot
 */
@AutoValue
public abstract class Shot implements HasStableId {

  public transient boolean hasFadedIn = false;

  public static JsonAdapter<Shot> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Shot.MoshiJsonAdapter(moshi);
  }

  public abstract boolean animated();

  @Json(name = "attachments_count")
  public abstract long attachmentsCount();

  @Json(name = "attachments_url")
  public abstract String attachmentsUrl();

  @Json(name = "buckets_count")
  public abstract long bucketsCount();

  @Json(name = "buckets_url")
  public abstract String bucketsUrl();

  @Json(name = "comments_count")
  public abstract long commentsCount();

  @Json(name = "comments_url")
  public abstract String commentsUrl();

  @Json(name = "created_at")
  public abstract Instant createdAt();

  @Nullable
  public abstract String description();

  public abstract long height();

  @Json(name = "html_url")
  public abstract String htmlUrl();

  public abstract long id();

  public abstract Images images();

  @Json(name = "likes_count")
  public abstract long likesCount();

  @Json(name = "likes_url")
  public abstract String likesUrl();

  @Json(name = "projects_url")
  public abstract String projectsUrl();

  @Json(name = "rebounds_count")
  public abstract long reboundsCount();

  @Json(name = "rebounds_url")
  public abstract String reboundsUrl();

  public abstract List<String> tags();

  @Nullable
  public abstract Team team();

  @Json(name = "updated_at")
  public abstract Instant updatedAt();

  public abstract User user();

  @Json(name = "views_count")
  public abstract long viewsCount();

  public abstract long width();

  @Override
  public long stableId() {
    return id();
  }
}
