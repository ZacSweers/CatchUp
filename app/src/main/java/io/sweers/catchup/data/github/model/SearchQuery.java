package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.threeten.bp.LocalDate;

import static org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE;

@AutoValue
public abstract class SearchQuery {
  public static JsonAdapter<SearchQuery> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_SearchQuery.MoshiJsonAdapter(moshi);
  }

  // Overkill for now, but can support other aspects in the future.
  public static Builder builder() {
    return new AutoValue_SearchQuery.Builder();
  }

  public abstract LocalDate createdSince();

  @Override
  public final String toString() {
    // Returning null here is not ideal, but it lets retrofit drop the query param altogether.
    return createdSince() == null ? null : "created:>=" + ISO_LOCAL_DATE.format(createdSince());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder createdSince(LocalDate createdSince);

    public abstract SearchQuery build();
  }
}
