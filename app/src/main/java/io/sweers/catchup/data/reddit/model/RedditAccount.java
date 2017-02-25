package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class RedditAccount extends RedditObject {
  public static JsonAdapter<RedditAccount> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_RedditAccount.MoshiJsonAdapter(moshi);
  }

  @Json(name = "comment_karma") public abstract int commentKarma();

  @Json(name = "has_mail") public abstract boolean hasMail();

  @Json(name = "has_mod_mail") public abstract boolean hasModMail();

  @Json(name = "has_verified_email") public abstract boolean hasVerifiedEmail();

  public abstract String id();

  @Json(name = "is_friend") public abstract boolean isFriend();

  @Json(name = "is_gold") public abstract boolean isGold();

  @Json(name = "is_mod") public abstract boolean isMod();

  @Json(name = "link_karma") public abstract int linkKarma();

  public abstract String modhash();

  public abstract String name();

  @Json(name = "over_18") public abstract boolean nsfw();
}
