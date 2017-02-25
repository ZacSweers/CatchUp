package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

@AutoValue
public abstract class RedditMore extends RedditObject {
  public static JsonAdapter<RedditMore> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditMore.MoshiJsonAdapter(moshi);
  }

  public abstract List<String> children();

  public abstract int count();

  public abstract String id();

  public abstract String name();

  @Json(name = "parent_id") public abstract String parentId();
}
