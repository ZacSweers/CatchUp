package io.sweers.catchup.data.hackernews.model;

import com.squareup.moshi.Json;

import io.sweers.catchup.util.Strings;

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
    name = Strings.capitalize(name);
    return name;
  }
}
