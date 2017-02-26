package io.sweers.catchup.data.reddit.model;

import com.squareup.moshi.Json;

public enum RedditType {

  @Json(name = "t1")
  T1(RedditComment.class),

  @Json(name = "t3")
  T3(RedditLink.class),

  @Json(name = "Listing")
  LISTING(RedditListing.class),

  @Json(name = "more")
  MORE(RedditMore.class);

  private final Class<?> clazz;

  RedditType(Class<?> cls) {
    clazz = cls;
  }

  public Class<?> getDerivedClass() {
    return clazz;
  }
}
