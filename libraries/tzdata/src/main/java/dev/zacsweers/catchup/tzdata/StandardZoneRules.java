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

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;

/**
 * The rules describing how the zone offset varies through the year and historically.
 *
 * <p>This class is used by the TZDB time-zone rules.
 *
 * <h3>Specification for implementors</h3>
 *
 * This class is immutable and thread-safe.
 */
@SuppressWarnings("NewApi")
public final class StandardZoneRules {

  /**
   * Reads the state from the stream.
   *
   * @param in the input stream, not null
   * @return the created object, not null
   * @throws IOException if an error occurs
   */
  public static ZoneRules readExternal(DataInput in) throws IOException {
    int stdSize = in.readInt();
    long[] stdTrans = new long[stdSize];
    for (int i = 0; i < stdSize; i++) {
      stdTrans[i] = SerCompat.readEpochSec(in);
    }
    ZoneOffset[] stdOffsets = new ZoneOffset[stdSize + 1];
    for (int i = 0; i < stdOffsets.length; i++) {
      stdOffsets[i] = SerCompat.readOffset(in);
    }
    int savSize = in.readInt();
    long[] savTrans = new long[savSize];
    for (int i = 0; i < savSize; i++) {
      savTrans[i] = SerCompat.readEpochSec(in);
    }
    ZoneOffset[] savOffsets = new ZoneOffset[savSize + 1];
    for (int i = 0; i < savOffsets.length; i++) {
      savOffsets[i] = SerCompat.readOffset(in);
    }
    int ruleSize = in.readByte();
    ZoneOffsetTransitionRule[] rules = new ZoneOffsetTransitionRule[ruleSize];
    for (int i = 0; i < ruleSize; i++) {
      rules[i] =
          (ZoneOffsetTransitionRule) SerCompat.readExternalFor(ZoneOffsetTransitionRule.class, in);
    }
    try {
      // TODO cache this
      Constructor<ZoneRules> constructor =
          ZoneRules.class.getDeclaredConstructor(
              long[].class,
              ZoneOffset[].class,
              long[].class,
              ZoneOffset[].class,
              ZoneOffsetTransitionRule[].class);
      return constructor.newInstance(stdTrans, stdOffsets, savTrans, savOffsets, rules);
    } catch (Exception e) {
      throw new IOException("No constructor found", e);
    }
  }
}
