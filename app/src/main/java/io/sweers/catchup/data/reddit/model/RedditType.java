package io.sweers.catchup.data.reddit.model;

public enum RedditType {
  t1(RedditComment.class),
  t3(RedditLink.class),
  Listing(RedditListing.class),
  more(RedditMore.class);

  private final Class mCls;

  RedditType(Class cls) {
    mCls = cls;
  }

  public Class getDerivedClass() {
    return mCls;
  }
}
