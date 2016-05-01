package io.sweers.catchup.model.medium;

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

  public abstract String id();

  public abstract String name();

  @Nullable public abstract String domain();
}
