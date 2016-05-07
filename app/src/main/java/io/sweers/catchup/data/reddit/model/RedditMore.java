package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@AutoValue
public abstract class RedditMore extends RedditObject {
  public static TypeAdapter<RedditMore> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditMore.GsonTypeAdapter(gson);
  }

  public abstract int count();

  @SerializedName("parent_id")
  public abstract String parentId();

  public abstract String id();

  public abstract String name();

  public abstract List<String> children();
}
