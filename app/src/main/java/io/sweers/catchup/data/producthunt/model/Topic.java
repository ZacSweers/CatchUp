package io.sweers.catchup.data.producthunt.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Topic {

  public static JsonAdapter<Topic> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Topic.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_Topic.Builder();
  }

  public abstract long id();

  public abstract String name();

  public abstract String slug();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder id(long id);

    public abstract Builder name(String name);

    public abstract Builder slug(String slug);

    public abstract Topic build();
  }

}
