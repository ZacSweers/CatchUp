package io.sweers.catchup.data.reddit.model;

import com.google.gson.annotations.SerializedName;

public enum RedditType {

  @SerializedName("t1")
  T1(RedditComment.class),

  @SerializedName("t3")
  T3(RedditLink.class),

  @SerializedName("Listing")
  LISTING(RedditListing.class),

  @SerializedName("more")
  MORE(RedditMore.class);

  private final Class clazz;

  RedditType(Class cls) {
    clazz = cls;
  }

  public Class getDerivedClass() {
    return clazz;
  }
}
