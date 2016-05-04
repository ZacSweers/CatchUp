package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class SearchRepositoriesResult {

  public static JsonAdapter<SearchRepositoriesResult> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_SearchRepositoriesResult.MoshiJsonAdapter(moshi);
  }

  public abstract int total_count();

  public abstract boolean incomplete_results();

  public abstract List<Repository> items();

}
