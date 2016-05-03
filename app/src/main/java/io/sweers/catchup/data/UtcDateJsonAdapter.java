package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

/**
 * Formats dates in UTC seconds time to {@link Date} instances.
 */
public final class UtcDateJsonAdapter extends JsonAdapter<Date> {

  private final boolean isSeconds;

  public UtcDateJsonAdapter() {
    this(false);
  }

  /**
   * @param isSeconds because some APIs give you UTC time in seconds
   */
  public UtcDateJsonAdapter(boolean isSeconds) {
    this.isSeconds = isSeconds;
  }

  @Override public synchronized Date fromJson(JsonReader reader) throws IOException {
    long l = reader.nextLong();
    if (isSeconds) {
      l *= 1000;
    }
    return new Date(l);
  }

  @Override public synchronized void toJson(JsonWriter writer, Date value) throws IOException {
    long longTime = value.getTime();
    if (isSeconds) {
      longTime /= 1000;
    }
    writer.value(longTime);
  }
}
