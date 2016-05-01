package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class MediumResponse {

  public static JsonAdapter<MediumResponse> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_MediumResponse.MoshiJsonAdapter(moshi);
  }

  public abstract boolean success();

  public abstract Payload payload();
}
