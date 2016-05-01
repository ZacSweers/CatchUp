package io.sweers.catchup.data.hackernews.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class HackerNewsStory {

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

  public abstract long time();

  public abstract String title();

  @Nullable public abstract String text();

  public abstract HNType type();

  @Nullable
  public abstract String url();

  public enum HNType {
    @Json(name = "job")
    JOB,

    @Json(name = "story")
    STORY,

    @Json(name = "comment")
    COMMENT,

    @Json(name = "poll")
    POLL,

    @Json(name = "pollopt")
    POLLOPT
  }
}
