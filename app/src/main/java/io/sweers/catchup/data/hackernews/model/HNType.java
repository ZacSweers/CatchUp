package io.sweers.catchup.data.hackernews.model;

import com.squareup.moshi.Json;

public enum HNType {
  @Json(name = "comment")
  COMMENT,

  @Json(name = "job")
  JOB,

  @Json(name = "poll")
  POLL,

  @Json(name = "pollopt")
  POLLOPT,

  @Json(name = "story")
  STORY;

  public String tag() {
    String name = name().toLowerCase();
    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    return name;
  }
}
