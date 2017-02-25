package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;
import com.squareup.moshi.Json;
import io.sweers.catchup.ui.base.HasStableId;
import org.threeten.bp.Instant;

public abstract class RedditSubmission extends RedditObject implements HasStableId {

  public abstract String author();

  @Nullable @Json(name = "author_flair_text") public abstract String authorFlairText();

  @Nullable @Json(name = "banned_by") public abstract String bannedBy();

  public abstract Instant created();

  @Json(name = "created_utc") public abstract Instant createdUtc();

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
