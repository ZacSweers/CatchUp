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
