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
  STORY
}
