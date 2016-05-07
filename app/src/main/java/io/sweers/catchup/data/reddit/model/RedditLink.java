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

  public abstract String domain();

  @Nullable @SerializedName("selftext_html")
  public abstract String selftextHtml();

  @Nullable public abstract String selftext();

  @Nullable @SerializedName("link_flair_text")
  public abstract String linkFlairText();

  public abstract boolean clicked();

  public abstract boolean hidden();

  public abstract String thumbnail();

  @SerializedName("is_self")
  public abstract boolean isSelf();

  public abstract String permalink();

  public abstract boolean stickied();

  public abstract String url();

  public abstract String title();

  @SerializedName("num_comments")
  public abstract int numComments();

  public abstract boolean visited();
}
