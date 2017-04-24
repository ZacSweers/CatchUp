/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.producthunt.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.sweers.catchup.ui.base.HasStableId;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
import org.threeten.bp.Instant;
import timber.log.Timber;

/**
 * Models a post on Product Hunt.
 */
@AutoValue
public abstract class Post implements HasStableId {

  public static JsonAdapter<Post> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Post.MoshiJsonAdapter(moshi);
  }

  public static Builder builder() {
    return new AutoValue_Post.Builder();
  }

  public abstract int comments_count();

  // TODO Coerce this to Instant - '2016-05-06T00:45:40.791-07:00'
  public abstract Instant created_at();

  public abstract String discussion_url();

  public abstract long id();

  public abstract List<User> makers();

  public abstract boolean maker_inside();

  public abstract String name();

  public abstract String redirect_url();

  public abstract Map<String, String> screenshot_url();

  public abstract String tagline();

  public abstract List<Topic> topics();

  public abstract User user();

  public abstract int votes_count();

  @Nullable
  public String getFirstTopic() {
    List<Topic> topics = topics();
    if (topics != null && !topics.isEmpty()) {
      return topics.get(0).name();
    }
    return null;
  }

  @Nullable
  public String getCategory() {
    String discussion = discussion_url();
    if (discussion != null) {
      return HttpUrl.parse(discussion).pathSegments().get(0);
    }
    return null;
  }

  @Nullable
  public String getScreenshotUrl(int width) {
    String url = null;
    for (String widthStr : screenshot_url().keySet()) {
      url = screenshot_url().get(widthStr);
      try {
        int screenshotWidth = Integer.parseInt(widthStr.substring(0, widthStr.length() -
            2));
        if (screenshotWidth > width) {
          break;
        }
      } catch (NumberFormatException nfe) {
        Timber.e(nfe, "FailedGetScreenshotUrl");
      }
    }

    return url;
  }

  @Override
  public long stableId() {
    return id();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder comments_count(int count);

    // TODO Coerce this to Instant - '2016-05-06T00:45:40.791-07:00'
    public abstract Builder created_at(Instant date);

    public abstract Builder discussion_url(String url);

    public abstract Builder id(long id);

    public abstract Builder makers(List<User> makers);

    public abstract Builder maker_inside(boolean makerInside);

    public abstract Builder name(String name);

    public abstract Builder redirect_url(String url);

    public abstract Builder screenshot_url(Map<String, String> url);

    public abstract Builder tagline(String tagline);

    public abstract Builder topics(List<Topic> topics);

    public abstract Builder user(User user);

    public abstract Builder votes_count(int count);

    public abstract Post build();
  }
}
