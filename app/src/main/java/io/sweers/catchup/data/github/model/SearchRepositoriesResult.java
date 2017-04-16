package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

@AutoValue
public abstract class SearchRepositoriesResult {

  public static JsonAdapter<SearchRepositoriesResult> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_SearchRepositoriesResult.MoshiJsonAdapter(moshi);
  }

  @Json(name = "incomplete_results") public abstract boolean incompleteResults();

  public abstract List<Repository> items();

  @Json(name = "total_count") public abstract int totalCount();
}
