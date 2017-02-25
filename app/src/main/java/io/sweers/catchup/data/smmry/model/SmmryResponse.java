package io.sweers.catchup.data.smmry.model;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
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
  @Json(name = "sm_api_title") @Nullable public abstract String title();

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
