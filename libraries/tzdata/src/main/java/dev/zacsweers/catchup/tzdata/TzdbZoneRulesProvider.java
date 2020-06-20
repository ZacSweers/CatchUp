/*
 * Copyright (C) 2020. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.catchup.tzdata;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.time.zone.ZoneRulesProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads time-zone rules for 'TZDB'.
 *
 * @since 1.8
 */
public final class TzdbZoneRulesProvider extends ZoneRulesProvider {

  /** All the regions that are available. */
  private List<String> regionIds;
  /** Version Id of this tzdb rules */
  private String versionId;
  /** Region to rules mapping */
  private final Map<String, Object> regionToRules = new ConcurrentHashMap<>();

  /**
   * Creates an instance. Created by the {@code ServiceLoader}.
   *
   * @throws ZoneRulesException if unable to load
   */
  // for desugar: load time zone rules as class loader resource
  public TzdbZoneRulesProvider() {
    try {
      URL datUrl =
          TzdbZoneRulesProvider.class
              .getClassLoader()
              .getResource(System.getProperty("jre.tzdb.dat", "j$/time/zone/tzdb.dat"));
      // Intentionally NPE on .openStream() if requisite resource is missing.
      DataInputStream dis = new DataInputStream(new BufferedInputStream(datUrl.openStream()));
      load(dis);
    } catch (Exception ex) {
      throw new ZoneRulesException("Unable to load TZDB time-zone rules", ex);
    }
  }

  @Override
  protected Set<String> provideZoneIds() {
    return new HashSet<>(regionIds);
  }

  @Override
  protected ZoneRules provideRules(String zoneId, boolean forCaching) {
    // forCaching flag is ignored because this is not a dynamic provider
    Object obj = regionToRules.get(zoneId);
    if (obj == null) {
      throw new ZoneRulesException("Unknown time-zone ID: " + zoneId);
    }
    try {
      if (obj instanceof byte[]) {
        byte[] bytes = (byte[]) obj;
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        obj = SerCompat.read(dis);
        regionToRules.put(zoneId, obj);
      }
      return (ZoneRules) obj;
    } catch (Exception ex) {
      throw new ZoneRulesException(
          "Invalid binary time-zone data: TZDB:" + zoneId + ", version: " + versionId, ex);
    }
  }

  @Override
  protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
    TreeMap<String, ZoneRules> map = new TreeMap<>();
    ZoneRules rules = getRules(zoneId, false);
    if (rules != null) {
      map.put(versionId, rules);
    }
    return map;
  }

  /**
   * Loads the rules from a DateInputStream, often in a jar file.
   *
   * @param dis the DateInputStream to load, not null
   * @throws Exception if an error occurs
   */
  private void load(DataInputStream dis) throws Exception {
    if (dis.readByte() != 1) {
      throw new StreamCorruptedException("File format not recognised");
    }
    // group
    String groupId = dis.readUTF();
    if ("TZDB".equals(groupId) == false) {
      throw new StreamCorruptedException("File format not recognised");
    }
    // versions
    int versionCount = dis.readShort();
    for (int i = 0; i < versionCount; i++) {
      versionId = dis.readUTF();
    }
    // regions
    int regionCount = dis.readShort();
    String[] regionArray = new String[regionCount];
    for (int i = 0; i < regionCount; i++) {
      regionArray[i] = dis.readUTF();
    }
    regionIds = Arrays.asList(regionArray);
    // rules
    int ruleCount = dis.readShort();
    Object[] ruleArray = new Object[ruleCount];
    for (int i = 0; i < ruleCount; i++) {
      byte[] bytes = new byte[dis.readShort()];
      dis.readFully(bytes);
      ruleArray[i] = bytes;
    }
    // link version-region-rules
    for (int i = 0; i < versionCount; i++) {
      int versionRegionCount = dis.readShort();
      regionToRules.clear();
      for (int j = 0; j < versionRegionCount; j++) {
        String region = regionArray[dis.readShort()];
        Object rule = ruleArray[dis.readShort() & 0xffff];
        regionToRules.put(region, rule);
      }
    }
  }

  @Override
  public String toString() {
    return "TZDB[" + versionId + "]";
  }
}
