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

package io.sweers.catchup.data.smmry.model;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import io.sweers.catchup.data.adapters.UnEscape;
import java.util.List;

@AutoValue
public abstract class SmmryResponse {

  /**
   * Contains notices, warnings, and error messages.
   */
  @Json(name = "sm_api_message") @Nullable public abstract String apiMessage();

  /**
   * Contains the amount of characters returned
   */
  @Json(name = "sm_api_character_count") @Nullable public abstract String characterCount();

  /**
   * Contains the title when available
   */
  @Json(name = "sm_api_title") @UnEscape @Nullable public abstract String title();

  /**
   * Contains the summary
   */
  @Json(name = "sm_api_content") @Nullable public abstract String content();

  /**
   * Contains top ranked keywords in descending order
   */
  @Json(name = "sm_api_keyword_array") @Nullable public abstract List<String> keywords();

  /**
   * Contains error code
   * 0 - Internal server problem which isn't your fault
   * 1 - Incorrect submission variables
   * 2 - Intentional restriction (low credits/disabled API key/banned API key)
   * 3 - Summarization error
   */
  @Json(name = "sm_api_error") public abstract int errorCode();

  public static JsonAdapter<SmmryResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_SmmryResponse.MoshiJsonAdapter(moshi);
  }
}
