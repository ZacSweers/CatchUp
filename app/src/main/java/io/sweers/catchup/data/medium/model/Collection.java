package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Collection {
  public static JsonAdapter<Collection> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Collection.MoshiJsonAdapter(moshi);
  }

  @Nullable
  public abstract String domain();

  public abstract String id();

  public abstract String name();
}
