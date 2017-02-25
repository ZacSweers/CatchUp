package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.threeten.bp.Instant;

/**
 * Formats dates in UTC seconds or milliseconds time to {@link Instant} instances.
 */
public final class EpochInstantJsonAdapter extends JsonAdapter<Instant> {

  private final TimeUnit timeUnit;

  public EpochInstantJsonAdapter() {
    this(TimeUnit.SECONDS);
  }

  /**
   * @param timeUnit because some APIs give you UTC time in different units
   */
  public EpochInstantJsonAdapter(TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  @Override public synchronized Instant fromJson(JsonReader reader) throws IOException {
    long l = reader.nextLong();
    return Instant.ofEpochMilli(TimeUnit.MILLISECONDS.convert(l, timeUnit));
  }

  @Override public synchronized void toJson(JsonWriter writer, Instant value) throws IOException {
    long longTime = value.toEpochMilli();
    writer.value(TimeUnit.MILLISECONDS.convert(longTime, timeUnit));
  }
}
