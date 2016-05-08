package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.Instant;

import io.sweers.catchup.ui.base.HasStableId;

public abstract class RedditSubmission extends RedditObject implements HasStableId {

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

  @Override public long stableId() {
    return id().hashCode();
  }
}
