package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Date;

@AutoValue
public abstract class Post {
  public static JsonAdapter<Post> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Post.MoshiJsonAdapter(moshi);
  }

  public abstract Date createdAt();

  public abstract String creatorId();

  public abstract String homeCollectionId();

  public abstract String id();

  public abstract String title();

  public abstract String uniqueSlug();

  public abstract Virtuals virtuals();
}
