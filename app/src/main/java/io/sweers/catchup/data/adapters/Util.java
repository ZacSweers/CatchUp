/*
 * Copyright 2014 Square, Inc.
 * Copyright 2016 Serj Lotutovici
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.data.adapters;

import android.support.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Set;

final class Util {
  @Nullable public static Annotation findAnnotation(Set<? extends Annotation> annotations,
      Class<? extends Annotation> annotationClass) {
    if (annotations.isEmpty()) return null; // Save an iterator in the common case.
    for (Annotation annotation : annotations) {
      if (annotation.annotationType() == annotationClass) {
        return annotation;
      }
    }
    return null;
  }

  public static boolean hasAnnotation(Set<? extends Annotation> annotations,
      Class<? extends Annotation> annotationClass) {
    if (annotations.isEmpty()) return false; // Save an iterator in the common case.
    for (Annotation annotation : annotations) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  private Util() {
    throw new AssertionError("No instances.");
  }
}
