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

package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Map;

@AutoValue
public abstract class References {
  public static JsonAdapter<References> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_References.MoshiJsonAdapter(moshi);
  }

  @Json(name = "Collection")
  public abstract Map<String, Collection> collection();

  @Json(name = "Post")
  public abstract Map<String, Post> post();

  @Json(name = "User")
  public abstract Map<String, User> user();
}
