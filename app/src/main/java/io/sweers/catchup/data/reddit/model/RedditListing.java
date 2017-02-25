package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

@AutoValue
public abstract class RedditListing extends RedditObject {
  public static JsonAdapter<RedditListing> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditListing.MoshiJsonAdapter(moshi);
  }

  public abstract String after();

  @Nullable public abstract String before();

  public abstract List<? extends RedditObject> children();

  public abstract String modhash();
}
