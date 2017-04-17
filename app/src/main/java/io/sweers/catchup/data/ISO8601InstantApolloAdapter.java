package io.sweers.catchup.data;

import com.apollographql.apollo.CustomTypeAdapter;
import io.sweers.catchup.util.Instants;
import org.threeten.bp.Instant;

/**
 * A CustomTypeAdapter for apollo that can convert ISO style date strings to Instant.
 */
public final class ISO8601InstantApolloAdapter implements CustomTypeAdapter<Instant> {
  @Override public Instant decode(String value) {
    return Instants.parsePossiblyOffsetInstant(value);
  }

  @Override public String encode(Instant instant) {
    return instant.toString();
  }
}
