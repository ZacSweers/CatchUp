package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Markup {
  public static JsonAdapter<Markup> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Markup.MoshiJsonAdapter(moshi);
  }

  public abstract int type();

  public abstract int start();

  public abstract int end();
}
