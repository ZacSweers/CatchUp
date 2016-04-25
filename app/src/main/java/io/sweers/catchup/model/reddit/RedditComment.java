package io.sweers.catchup.model.reddit;

public class RedditComment extends RedditSubmission {
  RedditObject replies;
  String subreddit_id;
  String parent_id;
  int controversiality;
  String body;
  String body_html;
  String link_id;
  int depth;

  public RedditObject getReplies() {
    return replies;
  }

  public String getSubredditId() {
    return subreddit_id;
  }

  public String getParentId() {
    return parent_id;
  }

  public int getControversiality() {
    return controversiality;
  }

  public String getBody() {
    return body;
  }

  public String getBodyHtml() {
    return body_html;
  }

  public String getLinkId() {
    return link_id;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }
}
