package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class RedditLink extends RedditSubmission {
  public static TypeAdapter<RedditLink> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditLink.GsonTypeAdapter(gson);
  }

  public abstract boolean clicked();

  public abstract String domain();

  public abstract boolean hidden();

  @SerializedName("is_self")
  public abstract boolean isSelf();

  @Nullable @SerializedName("link_flair_text")
  public abstract String linkFlairText();

  @SerializedName("num_comments")
  public abstract int commentsCount();

  public abstract String permalink();

  @Nullable public abstract String selftext();

  @Nullable @SerializedName("selftext_html")
  public abstract String selftextHtml();

  public abstract boolean stickied();

  public abstract String thumbnail();

  public abstract String title();

  public abstract String url();

  public abstract boolean visited();
}
