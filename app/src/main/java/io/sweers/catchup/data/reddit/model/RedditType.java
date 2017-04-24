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

package io.sweers.catchup.data.reddit.model;

import com.squareup.moshi.Json;

public enum RedditType {

  @Json(name = "t1")
  T1(RedditComment.class),

  @Json(name = "t3")
  T3(RedditLink.class),

  @Json(name = "Listing")
  LISTING(RedditListing.class),

  @Json(name = "more")
  MORE(RedditMore.class);

  private final Class<?> clazz;

  RedditType(Class<?> cls) {
    clazz = cls;
  }

  public Class<?> getDerivedClass() {
    return clazz;
  }
}
