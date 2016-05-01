package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Map;

@AutoValue
public abstract class References {
  public static JsonAdapter<References> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_References.MoshiJsonAdapter(moshi);
  }

  public abstract Map<String, Collection> Collection();

  public abstract Map<String, Post> Post();

  public abstract Map<String, User> User();
}
