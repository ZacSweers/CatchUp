package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.threeten.bp.Instant;

@AutoValue
public abstract class Repository {

  public static JsonAdapter<Repository> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Repository.MoshiJsonAdapter(moshi);
  }

  public abstract String name();

  public abstract String full_name();

  public abstract User owner();

  public abstract String html_url();

  public abstract Instant created_at();

  @Nullable public abstract String language();

  public abstract int stargazers_count();

}
