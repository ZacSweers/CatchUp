package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditResponse {
  public static JsonAdapter<RedditResponse> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditResponse.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_RedditResponse.Builder();
  }

  public abstract RedditListing data();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder data(RedditListing listing);

    public abstract RedditResponse build();
  }
}
