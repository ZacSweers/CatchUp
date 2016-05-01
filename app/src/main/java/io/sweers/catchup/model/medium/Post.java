package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class Post {
  public static JsonAdapter<Post> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Post.MoshiJsonAdapter(moshi);
  }

  public abstract long createdAt();

  public abstract String creatorId();

  public abstract String homeCollectionId();

  public abstract String id();

  public abstract String title();

  public abstract String uniqueSlug();

  public abstract Virtuals virtuals();
}
