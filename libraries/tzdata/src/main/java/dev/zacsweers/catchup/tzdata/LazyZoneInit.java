package dev.zacsweers.catchup.tzdata;

import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;

public final class LazyZoneInit {
  /**
   * Call on background thread to eagerly load all zones. Starts with loading
   * {@link ZoneId#systemDefault()} which is the one most likely to be used.
   */
  public static void cacheZones() {
    ZoneId.systemDefault().getRules();
    for (String zoneId : ZoneRulesProvider.getAvailableZoneIds()) {
      ZoneRulesProvider.getRules(zoneId, true);
    }
  }
}
