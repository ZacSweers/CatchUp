package com.gabrielittner.threetenbp;

import dev.zacsweers.catchup.tzdata.SerCompat;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static java.util.Objects.requireNonNull;

// In this package because that's where LazyZoneRules is generated to for now.
public final class LazyZoneRulesProvider extends ZoneRulesProvider {

  private final NavigableMap<String, ZoneRules> map = new ConcurrentSkipListMap<>();

  @Override protected Set<String> provideZoneIds() {
    return new HashSet<>(LazyZoneRules.REGION_IDS);
  }

  @Override protected ZoneRules provideRules(String zoneId, boolean forCaching) {
    requireNonNull(zoneId, "zoneId");
    ZoneRules rules = map.get(zoneId);
    if (rules == null) {
      rules = loadData(zoneId);
      map.put(zoneId, rules);
    }
    return rules;
  }

  @Override protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
    String versionId = LazyZoneRules.VERSION;
    ZoneRules rules = provideRules(zoneId, false);
    return new TreeMap<>(Collections.singletonMap(versionId, rules));
  }

  private ZoneRules loadData(String zoneId) {
    String fileName = "tzdb/" + zoneId + ".dat";
    InputStream is = null;
    try {
      URL datUrl = LazyZoneRulesProvider.class.getClassLoader()
          .getResource(fileName);
      // Intentionally NPE on .openStream() if requisite resource is missing.
      is = new DataInputStream(new BufferedInputStream(datUrl.openStream()));
      return loadData(is);
    } catch (Exception ex) {
      throw new ZoneRulesException("Invalid binary time-zone data: " + fileName, ex);
    } finally {
      close(is);
    }
  }

  private ZoneRules loadData(InputStream in) throws Exception {
    DataInputStream dis = new DataInputStream(in);
    if (dis.readByte() != 1) {
      throw new StreamCorruptedException("File format not recognised");
    }

    String groupId = dis.readUTF();
    if (!"TZDB-ZONE".equals(groupId)) {
      throw new StreamCorruptedException("File format not recognised");
    }
    return (ZoneRules) SerCompat.read(dis);
  }

  private void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}