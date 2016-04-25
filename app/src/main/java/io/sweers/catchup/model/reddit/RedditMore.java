package io.sweers.catchup.model.reddit;

import java.util.List;

public class RedditMore extends RedditObject {
  int count;
  String parent_id;
  String id;
  String name;
  List<String> children;

  public int getCount() {
    return count;
  }

  public String getParentId() {
    return parent_id;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<String> getChildren() {
    return children;
  }
}
