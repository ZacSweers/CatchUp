package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

@AutoValue
public abstract class RedditListing extends RedditObject {
  public static TypeAdapter<RedditListing> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditListing.GsonTypeAdapter(gson);
  }

  public abstract String modhash();

  public abstract String after();

  @Nullable public abstract String before();

  public abstract List<? extends RedditObject> children();
}
