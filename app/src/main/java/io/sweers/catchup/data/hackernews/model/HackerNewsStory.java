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

package io.sweers.catchup.data.hackernews.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.sweers.catchup.ui.base.HasStableId;
import java.util.List;
import org.threeten.bp.Instant;

@AutoValue
public abstract class HackerNewsStory implements HasStableId {

  public static JsonAdapter<HackerNewsStory> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_HackerNewsStory.MoshiJsonAdapter(moshi);
  }

  public abstract String by();

  public abstract boolean dead();

  public abstract boolean deleted();

  public abstract int descendants();

  public abstract String id();

  @Nullable public abstract List<String> kids();

  @Nullable public abstract HackerNewsStory parent();

  @Nullable public abstract List<String> parts();

  public abstract int score();

  public abstract Instant time();

  public abstract String title();

  @Nullable public abstract String text();

  public abstract HNType type();

  @Nullable public abstract String url();

  @Override public long stableId() {
    return id().hashCode();
  }
}
