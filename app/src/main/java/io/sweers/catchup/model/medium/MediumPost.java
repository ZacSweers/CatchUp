package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class MediumPost {
  public static JsonAdapter<MediumPost> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_MediumPost.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_MediumPost.Builder();
  }

  @Nullable public abstract Collection collection();

  public abstract Post post();

  public abstract User user();

  @NonNull
  public String constructUrl() {
    return "https://medium.com/@" + user().username() + "/" + post().uniqueSlug();
  }

  @NonNull
  public String constructCommentsUrl() {
    return constructUrl() + "#--responses";
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder collection(@Nullable Collection value);

    public abstract Builder post(Post value);

    public abstract Builder user(User value);

    public abstract MediumPost build();
  }
}
