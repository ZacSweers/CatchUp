package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Repository {

  public static JsonAdapter<Repository> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Repository.MoshiJsonAdapter(moshi);
  }

  @Json(name = "created_at")
  public abstract Instant createdAt();

  @Json(name = "full_name")
  public abstract String fullName();

  @Json(name = "html_url")
  public abstract String htmlUrl();

  @Nullable public abstract String language();

  public abstract String name();

  public abstract User owner();

  @Json(name = "stargazers_count")
  public abstract int starsCount();

}
