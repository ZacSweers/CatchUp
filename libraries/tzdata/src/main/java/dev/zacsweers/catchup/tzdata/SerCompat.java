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
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;

/**
 * The shared serialization delegate for this package.
 *
 * @implNote This class is mutable and should be created once per serialization.
 * @serial include
 * @since 1.8
 */
// Copied from desugar's implementation
public final class SerCompat implements Externalizable {

  /** Serialization version. */
  private static final long serialVersionUID = -8885321777449118786L;

  /** Type for ZoneRules. */
  static final byte ZRULES = 1;
  /** Type for ZoneOffsetTransition. */
  static final byte ZOT = 2;
  /** Type for ZoneOffsetTransition. */
  static final byte ZOTRULE = 3;

  /** The type being serialized. */
  private byte type;
  /** The object being serialized. */
  private Object object;

  /** Constructor for deserialization. */
  public SerCompat() {}

  // -----------------------------------------------------------------------

  /**
   * Implements the {@code Externalizable} interface to write the object.
   *
   * @param out the data stream to write to, not null
   * @serialData Each serializable class is mapped to a type that is the first byte in the stream.
   *     Refer to each class {@code writeReplace} serialized form for the value of the type and
   *     sequence of values for the type.
   *     <ul>
   *       <li><a
   *           href="../../../serialized-form.html#java.time.zone.ZoneRules">ZoneRules.writeReplace</a>
   *       <li><a
   *           href="../../../serialized-form.html#java.time.zone.ZoneOffsetTransition">ZoneOffsetTransition.writeReplace</a>
   *       <li><a
   *           href="../../../serialized-form.html#java.time.zone.ZoneOffsetTransitionRule">ZoneOffsetTransitionRule.writeReplace</a>
   *     </ul>
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    writeInternal(type, object, out);
  }

  private static void writeInternal(byte type, Object object, DataOutput out) throws IOException {
    out.writeByte(type);
    switch (type) {
      case ZRULES:
        writeExternalFor(ZoneRules.class, object, out);
        break;
      case ZOT:
        writeExternalFor(ZoneOffsetTransition.class, object, out);
        break;
      case ZOTRULE:
        writeExternalFor(ZoneOffsetTransitionRule.class, object, out);
        break;
      default:
        throw new InvalidClassException("Unknown serialized type");
    }
  }

  // Reflection is necessary for compatibility with D8
  private static void writeExternalFor(Class<?> clazz, Object instance, DataOutput out) {
    try {
      Method method = clazz.getDeclaredMethod("writeExternal", DataOutput.class);
      method.setAccessible(true);
      method.invoke(instance, out);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------

  /**
   * Implements the {@code Externalizable} interface to read the object.
   *
   * @param in the data to read, not null
   * @serialData The streamed type and parameters defined by the type's {@code writeReplace} method
   *     are read and passed to the corresponding static factory for the type to create a new
   *     instance. That instance is returned as the de-serialized {@code Ser} object.
   *     <ul>
   *       <li><a href="../../../serialized-form.html#java.time.zone.ZoneRules">ZoneRules</a> -
   *           {@code ZoneRules.of(standardTransitions, standardOffsets, savingsInstantTransitions,
   *           wallOffsets, lastRules);}
   *       <li><a
   *           href="../../../serialized-form.html#java.time.zone.ZoneOffsetTransition">ZoneOffsetTransition</a>
   *           - {@code ZoneOffsetTransition of(LocalDateTime.ofEpochSecond(epochSecond),
   *           offsetBefore, offsetAfter);}
   *       <li><a
   *           href="../../../serialized-form.html#java.time.zone.ZoneOffsetTransitionRule">ZoneOffsetTransitionRule</a>
   *           - {@code ZoneOffsetTransitionRule.of(month, dom, dow, time, timeEndOfDay,
   *           timeDefinition, standardOffset, offsetBefore, offsetAfter);}
   *     </ul>
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException {
    type = in.readByte();
    object = readInternal(type, in);
  }

  public static Object read(DataInput in) throws IOException {
    byte type = in.readByte();
    return readInternal(type, in);
  }

  private static Object readInternal(byte type, DataInput in) throws IOException {
    switch (type) {
      case ZRULES:
        return readExternalFor(ZoneRules.class, in);
      case ZOT:
        return readExternalFor(ZoneOffsetTransition.class, in);
      case ZOTRULE:
        return readExternalFor(ZoneOffsetTransitionRule.class, in);
      default:
        throw new StreamCorruptedException("Unknown serialized type");
    }
  }

  // Reflection is necessary for compatibility with D8
  private static Object readExternalFor(Class<?> clazz, DataInput in) {
    Method method;
    try {
      method = clazz.getDeclaredMethod("readExternal", DataInput.class);
      method.setAccessible(true);
      return method.invoke(null, in);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the object that will replace this one.
   *
   * @return the read object, should never be null
   */
  private Object readResolve() {
    return object;
  }
}
