package io.sweers.catchup.data.reddit;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;

/**
 * Converts timestamp longs to Dates
 */
public class UtcDateTypeAdapter extends TypeAdapter<Date> {

  private final boolean isSeconds;

  public UtcDateTypeAdapter() {
    this(false);
  }

  /**
   * @param isSeconds because some APIs give you UTC time in seconds
   */
  public UtcDateTypeAdapter(boolean isSeconds) {
    this.isSeconds = isSeconds;
  }

  @Override public void write(JsonWriter out, Date value) throws IOException {
    long l = value.getTime();
    if (isSeconds) {
      l /= 1000;
    }
    out.value(l);
  }

  @Override public Date read(JsonReader in) throws IOException {
    long l = in.nextLong();
    if (isSeconds) {
      l *= 1000;
    }
    return new Date(l);
  }
}
