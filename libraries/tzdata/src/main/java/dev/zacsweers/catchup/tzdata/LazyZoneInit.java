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

import java.time.ZoneId;
import java.time.zone.ZoneRulesProvider;

public final class LazyZoneInit {

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
