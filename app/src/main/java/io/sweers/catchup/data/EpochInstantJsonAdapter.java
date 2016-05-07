package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import org.threeten.bp.Instant;

import java.io.IOException;

/**
 * Formats dates in UTC seconds or milliseconds time to {@link Instant} instances.
 */
public final class EpochInstantJsonAdapter extends JsonAdapter<Instant> {

  private final boolean isSeconds;

  public EpochInstantJsonAdapter() {
    this(false);
  }

  /**
   * @param isSeconds because some APIs give you UTC time in seconds
   */
  public EpochInstantJsonAdapter(boolean isSeconds) {
    this.isSeconds = isSeconds;
  }

  @Override public synchronized Instant fromJson(JsonReader reader) throws IOException {
    long l = reader.nextLong();
    if (isSeconds) {
      l *= 1000;
    }
    return Instant.ofEpochMilli(l);
  }

  @Override public synchronized void toJson(JsonWriter writer, Instant value) throws IOException {
    long longTime = value.toEpochMilli();
    if (isSeconds) {
      longTime /= 1000;
    }
    writer.value(longTime);
  }
}
