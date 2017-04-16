package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import io.sweers.catchup.util.Instants;
import java.io.IOException;
import org.threeten.bp.Instant;

public final class ISO8601InstantAdapter extends JsonAdapter<Instant> {

  @Override public Instant fromJson(JsonReader reader) throws IOException {
    String time = reader.nextString();
    return Instants.parsePossiblyOffsetInstant(time);
  }

  @Override public void toJson(JsonWriter writer, Instant instant) throws IOException {
    writer.value(instant.toString());
  }
}
