package io.sweers.catchup.data.reddit.model;

import com.google.gson.JsonElement;

public class RedditObjectWrapper {
  RedditType kind;
  JsonElement data;

  public RedditType getKind() {
    return kind;
  }

  public JsonElement getData() {
    return data;
  }
}
