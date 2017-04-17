package io.sweers.catchup.data.github.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.sweers.catchup.ui.base.HasStableId;
import org.threeten.bp.Instant;

@AutoValue
public abstract class Repository implements HasStableId {

  public static JsonAdapter<Repository> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Repository.MoshiJsonAdapter(moshi);
  }

  @Json(name = "created_at") public abstract Instant createdAt();

  @Json(name = "full_name") public abstract String fullName();

  @Json(name = "html_url") public abstract String htmlUrl();

  public abstract long id();

  @Nullable public abstract String language();

  public abstract String name();

  public abstract User owner();

  @Json(name = "stargazers_count") public abstract int starsCount();

  @Override public long stableId() {
    return id();
  }

  public static Builder builder() {
    return new AutoValue_Repository.Builder();
  }

  @AutoValue.Builder
  public interface Builder {
    Builder createdAt(Instant createdAt);

    Builder fullName(String fullName);

    Builder htmlUrl(String htmlUrl);

    Builder id(long id);

    Builder language(@Nullable String language);

    Builder name(String name);

    Builder owner(User owner);

    Builder starsCount(int starsCount);

    Repository build();
  }
}
