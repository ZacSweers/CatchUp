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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditLink extends RedditSubmission {
  public static JsonAdapter<RedditLink> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditLink.MoshiJsonAdapter(moshi);
  }

  public abstract boolean clicked();

  public abstract String domain();

  public abstract boolean hidden();

  @Json(name = "is_self") public abstract boolean isSelf();

  @Nullable @Json(name = "link_flair_text") public abstract String linkFlairText();

  @Json(name = "num_comments") public abstract int commentsCount();

  public abstract String permalink();

  @Nullable public abstract String selftext();

  @Nullable @Json(name = "selftext_html") public abstract String selftextHtml();

  public abstract boolean stickied();

  public abstract String thumbnail();

  public abstract String title();

  public abstract String url();

  public abstract boolean visited();
}
