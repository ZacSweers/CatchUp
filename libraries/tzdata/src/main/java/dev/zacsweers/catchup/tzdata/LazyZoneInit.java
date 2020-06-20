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
   * backdoor, since the Android implementation does not respect the system property like the JDK
   * or desugar versions do.
   *
   * ZoneRulesProvider is a tricky thing, as it's hidden from the Android API but does exist in the
   * JDK. It's not on any API grey/black lists but if you try to query it via reflection, it's
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
    Method registerProvider = ZoneRulesProvider.class.getDeclaredMethod("registerProvider", ZoneRulesProvider.class);
    registerProvider.setAccessible(true);
    providers.clear();
    zones.clear();
    registerProvider.invoke(null, new TzdbZoneRulesProvider());
    System.out.println("Backdoor success");
  }

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
