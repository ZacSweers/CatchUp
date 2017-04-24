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

package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;
import com.squareup.moshi.Json;
import io.sweers.catchup.ui.base.HasStableId;
import org.threeten.bp.Instant;

public abstract class RedditSubmission extends RedditObject implements HasStableId {

  public abstract String author();

  @Nullable @Json(name = "author_flair_text") public abstract String authorFlairText();

  @Nullable @Json(name = "banned_by") public abstract String bannedBy();

  public abstract Instant created();

  @Json(name = "created_utc") public abstract Instant createdUtc();

  public abstract int gilded();

  public abstract String id();

  public abstract String name();

  public abstract boolean saved();

  public abstract int score();

  public abstract String subreddit();

  public abstract int ups();

  @Override public long stableId() {
    return id().hashCode();
  }
}
