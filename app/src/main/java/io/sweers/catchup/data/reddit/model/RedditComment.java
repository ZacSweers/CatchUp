package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class RedditComment extends RedditSubmission {
  public static TypeAdapter<RedditComment> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditComment.GsonTypeAdapter(gson);
  }

  public abstract String body();

  @SerializedName("body_html")
  public abstract String bodyHtml();

  public abstract int controversiality();

  public abstract int depth();

  @SerializedName("link_id")
  public abstract String linkId();

  @SerializedName("parent_id")
  public abstract String parentId();

  public abstract RedditObject replies();

  @SerializedName("subreddit_id")
  public abstract String subredditId();
}
