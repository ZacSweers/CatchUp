package io.sweers.catchup.data;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import org.threeten.bp.Instant;

public final class ISOInstantAdapter {
  @ToJson
  public String toJson(Instant instant) {
    return instant.toString();
  }

  @FromJson
  public Instant fromJson(String value) {
    return Instant.parse(value);
  }
}
