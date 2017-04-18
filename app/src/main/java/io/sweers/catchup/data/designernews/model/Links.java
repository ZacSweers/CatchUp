package io.sweers.catchup.data.designernews.model;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

@AutoValue
public abstract class Links {

  public abstract String user();

  public abstract List<String> comments();

  public abstract List<String> upvotes();

  public abstract List<String> downvotes();

  public static JsonAdapter<Links> jsonAdapter(Moshi moshi) {
    return new AutoValue_Links.MoshiJsonAdapter(moshi);
  }
}
