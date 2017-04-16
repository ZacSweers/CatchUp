package io.sweers.catchup.data;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;

public final class ISOInstantAdapter extends JsonAdapter<Instant> {

  @Override public Instant fromJson(JsonReader reader) throws IOException {
    String time = reader.nextString();
    if (!time.endsWith("Z")) {
      return OffsetDateTime.parse(time)
          .toInstant();
    } else {
      return Instant.parse(time);
    }
  }

  @Override public void toJson(JsonWriter writer, Instant instant) throws IOException {
    writer.value(instant.toString());
  }
}
