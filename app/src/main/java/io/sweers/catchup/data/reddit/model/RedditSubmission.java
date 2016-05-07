package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.Instant;

public abstract class RedditSubmission extends RedditObject {

  public abstract String author();

  @Nullable @SerializedName("author_flair_text")
  public abstract String authorFlairText();

  @Nullable @SerializedName("banned_by")
  public abstract String bannedBy();

  public abstract Instant created();

  @SerializedName("created_utc")
  public abstract Instant createdUtc();

  public abstract int gilded();

  public abstract String id();

  public abstract String name();

  public abstract boolean saved();

  public abstract int score();

  public abstract String subreddit();

  public abstract int ups();
}
