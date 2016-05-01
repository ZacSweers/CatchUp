package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Virtuals {
  public static JsonAdapter<Virtuals> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Virtuals.MoshiJsonAdapter(moshi);
  }

  public abstract int recommends();

  public abstract int responsesCreatedCount();
}
