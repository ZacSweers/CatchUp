package io.sweers.catchup.data.reddit.model;

public class RedditAccount extends RedditObject {
  int comment_karma;
  boolean has_mail;
  boolean has_mod_mail;
  boolean has_verified_email;
  String id;
  boolean is_friend;
  boolean is_gold;
  boolean is_mod;
  int link_karma;
  String modhash;
  String name;
  boolean over_18;

  public int getCommentKarma() {
    return comment_karma;
  }

  public boolean hasMail() {
    return has_mail;
  }

  public boolean hasModMail() {
    return has_mod_mail;
  }

  public boolean hasVerifiedEmail() {
    return has_verified_email;
  }

  public String getId() {
    return id;
  }

  public boolean isFriend() {
    return is_friend;
  }

  public boolean isGold() {
    return is_gold;
  }

  public boolean isMod() {
    return is_mod;
  }

  public int getLinkKarma() {
    return link_karma;
  }

  public String getModhash() {
    return modhash;
  }

  public String getName() {
    return name;
  }

  public boolean isOver18() {
    return over_18;
  }
}
