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
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditResponse {
  public static JsonAdapter<RedditResponse> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditResponse.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_RedditResponse.Builder();
  }

  public abstract RedditListing data();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder data(RedditListing listing);

    public abstract RedditResponse build();
  }
}
