package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class User {
  public static JsonAdapter<User> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_User.MoshiJsonAdapter(moshi);
  }

  public abstract String userId();

  public abstract String username();

  public abstract String name();
}
