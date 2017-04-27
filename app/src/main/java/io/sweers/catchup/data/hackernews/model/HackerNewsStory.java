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

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.firebase.database.DataSnapshot;
import io.sweers.catchup.ui.base.HasStableId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import me.mattlogan.auto.value.firebase.adapter.FirebaseAdapter;
import me.mattlogan.auto.value.firebase.adapter.TypeAdapter;
import me.mattlogan.auto.value.firebase.annotation.FirebaseValue;
import org.threeten.bp.Instant;

@AutoValue
@FirebaseValue
public abstract class HackerNewsStory implements HasStableId {

  public abstract String by();

  public abstract boolean dead();

  public abstract boolean deleted();

  public abstract int descendants();

  public abstract long id();

  @Nullable public abstract List<Long> kids();

  @Nullable public abstract HackerNewsStory parent();

  @Nullable public abstract List<String> parts();

  public abstract int score();

  @FirebaseAdapter(InstantAdapter.class) public abstract Instant time();

  public abstract String title();

  @Nullable public abstract String text();

  @FirebaseAdapter(HNTypeAdapter.class) public abstract HNType type();

  @Nullable public abstract String url();

  @Override public long stableId() {
    return id();
  }

  public static HackerNewsStory create(DataSnapshot dataSnapshot) {
    return dataSnapshot.getValue(AutoValue_HackerNewsStory.FirebaseValue.class)
        .toAutoValue();
  }

  public static class InstantAdapter implements TypeAdapter<Instant, Long> {
    @Override public Instant fromFirebaseValue(Long value) {
      return Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(value, TimeUnit.SECONDS));
    }

    @Override public Long toFirebaseValue(Instant value) {
      long longTime = value.toEpochMilli();
      return TimeUnit.MILLISECONDS.convert(longTime, TimeUnit.SECONDS);
    }
  }

  public static class HNTypeAdapter implements TypeAdapter<HNType, String> {
    @Override public HNType fromFirebaseValue(String value) {
      return HNType.valueOf(value.toUpperCase(Locale.US));
    }

    @Override public String toFirebaseValue(HNType value) {
      return value.name()
          .toLowerCase(Locale.US);
    }
  }
}
