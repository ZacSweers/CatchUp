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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;
import java.util.List;
import java.util.Map;

public final class LazyZoneInit {

  /**
   * This is a (failed) attempted to demo how to install our ZoneRulesProvider via reflection
   * backdoor, since the Android implementation does not respect the system property like the JDK or
   * desugar versions do.
   *
   * <p>ZoneRulesProvider is a tricky thing, as it's hidden from the Android API but does exist in
   * the JDK. It's not on any API grey/black lists but if you try to query it via reflection, it's
   * opaque and says it has no fields or methods. This includes things like `getAvailableZoneIds()`,
   * which would then just fail at runtime!
   */
  public static void backdoorInstallForMinSdk26OrHigher()
      throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
          InvocationTargetException {
    System.out.println("Starting backdoor");
    // Clear the existing PROVIDERS list and ZONES map
    // Register ours via registerProvider
    Field providersField = ZoneRulesProvider.class.getDeclaredField("PROVIDERS");
    providersField.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<ZoneRulesProvider> providers = (List<ZoneRulesProvider>) providersField.get(null);
    Field zonesField = ZoneRulesProvider.class.getDeclaredField("ZONES");
    zonesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, ZoneRulesProvider> zones = (Map<String, ZoneRulesProvider>) zonesField.get(null);
    Method registerProvider =
        ZoneRulesProvider.class.getDeclaredMethod("registerProvider", ZoneRulesProvider.class);
    registerProvider.setAccessible(true);
    providers.clear();
    zones.clear();
    registerProvider.invoke(null, new TzdbZoneRulesProvider());
    System.out.println("Backdoor success");
  }

  /**
   * Call on background thread to eagerly load all zones. Starts with loading {@link
   * ZoneId#systemDefault()} which is the one most likely to be used.
   */
  public static void cacheZones() {
    ZoneId.systemDefault().getRules();
    for (String zoneId : ZoneRulesProvider.getAvailableZoneIds()) {
      ZoneRulesProvider.getRules(zoneId, true);
    }
  }
}
