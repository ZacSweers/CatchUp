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

package io.sweers.catchup.data.github.model;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import org.threeten.bp.LocalDate;

import static org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE;

@AutoValue
public abstract class SearchQuery {
  public static Builder builder() {
    return new AutoValue_SearchQuery.Builder();
  }

  @Nullable public abstract LocalDate createdSince();

  public abstract int minStars();

  @Override public final String toString() {
    // Returning null here is not ideal, but it lets retrofit drop the query param altogether.
    StringBuilder builder = new StringBuilder();
    if (createdSince() != null) {
      builder.append("created:>=")
          .append(ISO_LOCAL_DATE.format(createdSince()))
          .append(' ');
    }
    if (minStars() != 0) {
      builder.append("stars:>=")
          .append(minStars());
    }
    return builder.toString()
        .trim();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder createdSince(LocalDate createdSince);

    public abstract Builder minStars(int minStars);

    public abstract SearchQuery build();
  }
}
