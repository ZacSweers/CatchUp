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

package io.sweers.catchup.data;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import com.google.auto.value.AutoValue;
import org.threeten.bp.Instant;

@AutoValue
public abstract class CatchUpItem {

  public abstract CharSequence title();

  @Nullable public abstract Pair<String, Integer> score();

  public abstract Instant timestamp();

  @Nullable public abstract String tag();

  public abstract CharSequence author();

  public abstract CharSequence source();

  public abstract int commentCount();

  public abstract boolean hideComments();

  public abstract String itemClickUrl();

  public abstract String itemCommentClickUrl();

  public static Builder builder() {
    return new AutoValue_CatchUpItem.Builder();
  }

  @AutoValue.Builder
  public interface Builder {
    Builder title(CharSequence title);

    Builder score(@Nullable Pair<String, Integer> score);

    Builder timestamp(Instant timestamp);

    Builder tag(@Nullable String tag);

    Builder author(CharSequence author);

    Builder source(CharSequence source);

    Builder commentCount(int commentCount);

    Builder hideComments(boolean hideComments);

    Builder itemClickUrl(String itemClickUrl);

    Builder itemCommentClickUrl(String itemCommentClickUrl);

    CatchUpItem build();
  }
}
