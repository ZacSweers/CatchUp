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
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditComment extends RedditSubmission {
  public static JsonAdapter<RedditComment> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditComment.MoshiJsonAdapter(moshi);
  }

  public abstract String body();

  @Json(name = "body_html") public abstract String bodyHtml();

  public abstract int controversiality();

  public abstract int depth();

  @Json(name = "link_id") public abstract String linkId();

  @Json(name = "parent_id") public abstract String parentId();

  /**
   * Ugh-asaurus
   *
   * @return list of comments. Or false. Because yeah.
   */
  public abstract RedditObject replies();

  @Json(name = "subreddit_id") public abstract String subredditId();
}
