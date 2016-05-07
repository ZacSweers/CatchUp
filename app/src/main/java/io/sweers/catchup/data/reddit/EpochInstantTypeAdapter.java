package io.sweers.catchup.data.reddit;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.threeten.bp.Instant;

import java.io.IOException;

/**
 * Converts timestamp longs to Dates
 */
public class EpochInstantTypeAdapter extends TypeAdapter<Instant> {

  private final boolean isSeconds;

  public EpochInstantTypeAdapter() {
    this(false);
  }

  /**
   * @param isSeconds because some APIs give you UTC time in seconds
   */
  public EpochInstantTypeAdapter(boolean isSeconds) {
    this.isSeconds = isSeconds;
  }

  @Override public void write(JsonWriter out, Instant value) throws IOException {
    long l = value.toEpochMilli();
    if (isSeconds) {
      l /= 1000;
    }
    out.value(l);
  }

  @Override public Instant read(JsonReader in) throws IOException {
    long l = in.nextLong();
    if (isSeconds) {
      l *= 1000;
    }
    return Instant.ofEpochMilli(l);
  }
}
