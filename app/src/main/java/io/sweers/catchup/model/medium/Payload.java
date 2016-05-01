package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Payload {

  public static JsonAdapter<Payload> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Payload.MoshiJsonAdapter(moshi);
  }

  public abstract List<StreamItem> streamItems();

  public abstract References references();
}
