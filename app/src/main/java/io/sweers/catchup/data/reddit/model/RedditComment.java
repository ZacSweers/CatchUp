package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditComment extends RedditSubmission {
  public static JsonAdapter<RedditComment> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditComment.MoshiJsonAdapter(moshi);
  }

  public abstract String body();

  @Json(name = "body_html") public abstract String bodyHtml();

  public abstract int controversiality();

  public abstract int depth();

  @Json(name = "link_id") public abstract String linkId();

  @Json(name = "parent_id") public abstract String parentId();

  /**
   * Ugh-asaurus
   *
   * @return list of comments. Or false. Because yeah.
   */
  public abstract RedditObject replies();

  @Json(name = "subreddit_id") public abstract String subredditId();
}
