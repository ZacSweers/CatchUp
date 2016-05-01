package io.sweers.catchup.data.reddit.model;

import java.util.List;

public class RedditListing extends RedditObject {
  String modhash;
  String after;
  String before;
  List<? extends RedditObject> children;

  public String getModhash() {
    return modhash;
  }

  public String getAfter() {
    return after;
  }

  public String getBefore() {
    return before;
  }

  public List<? extends RedditObject> getChildren() {
    return children;
  }
}
