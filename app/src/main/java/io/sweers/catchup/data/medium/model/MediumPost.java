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

package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import io.sweers.catchup.ui.base.HasStableId;

@AutoValue
public abstract class MediumPost implements HasStableId {
  public static JsonAdapter<MediumPost> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_MediumPost.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_MediumPost.Builder();
  }

  @Nullable
  public abstract Collection collection();

  public abstract Post post();

  public abstract User user();

  @NonNull
  public String constructUrl() {
    return "https://medium.com/@" + user().username() + "/" + post().uniqueSlug();
  }

  @NonNull
  public String constructCommentsUrl() {
    return constructUrl() + "#--responses";
  }

  @Override
  public long stableId() {
    return post().id().hashCode();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder collection(@Nullable Collection value);

    public abstract Builder post(Post value);

    public abstract Builder user(User value);

    public abstract MediumPost build();
  }
}
