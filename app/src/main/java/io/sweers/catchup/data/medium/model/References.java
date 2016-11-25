package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Map;

@AutoValue
public abstract class References {
  public static JsonAdapter<References> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_References.MoshiJsonAdapter(moshi);
  }

  @Json(name = "Collection")
  public abstract Map<String, Collection> collection();

  @Json(name = "Post")
  public abstract Map<String, Post> post();

  @Json(name = "User")
  public abstract Map<String, User> user();
}
