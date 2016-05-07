package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RedditResponse {
  public static TypeAdapter<RedditResponse> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditResponse.GsonTypeAdapter(gson);
  }

  public abstract RedditListing data();
}
