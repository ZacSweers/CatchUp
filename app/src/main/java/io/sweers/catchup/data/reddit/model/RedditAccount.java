package io.sweers.catchup.data.reddit.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class RedditAccount extends RedditObject {
  public static TypeAdapter<RedditAccount> typeAdapter(@NonNull Gson gson) {
    return new AutoValue_RedditAccount.GsonTypeAdapter(gson);
  }

  @SerializedName("comment_karma")
  public abstract int commentKarma();

  @SerializedName("has_mail")
  public abstract boolean hasMail();

  @SerializedName("has_mod_mail")
  public abstract boolean hasModMail();

  @SerializedName("has_verified_email")
  public abstract boolean hasVerifiedEmail();

  public abstract String id();

  @SerializedName("is_friend")
  public abstract boolean isFriend();

  @SerializedName("is_gold")
  public abstract boolean isGold();

  @SerializedName("is_mod")
  public abstract boolean isMod();

  @SerializedName("link_karma")
  public abstract int linkKarma();

  public abstract String modhash();

  public abstract String name();

  @SerializedName("over_18")
  public abstract boolean nsfw();
}
