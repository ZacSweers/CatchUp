/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.preferences

import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences

inline fun <reified T> RxSharedPreferences.observe(key: String,
    defaultValue: T? = null): Preference<T> {
  return when (val targetClass = T::class.java) {
    Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> {
      defaultValue?.let {
        getBoolean(key, it as Boolean)
      } ?: getBoolean(key)
    }
    Integer::class.javaPrimitiveType, Integer::class.javaObjectType -> {
      defaultValue?.let {
        getInteger(key, it as Int)
      } ?: getInteger(key)
    }
    Float::class.javaPrimitiveType, Float::class.javaObjectType -> {
      defaultValue?.let {
        getFloat(key, it as Float)
      } ?: getFloat(key)
    }
    Long::class.javaPrimitiveType, Long::class.javaObjectType -> {
      defaultValue?.let {
        getLong(key, it as Long)
      } ?: getLong(key)
    }
    String::class.java -> {
      defaultValue?.let {
        getString(key, it as String)
      } ?: getString(key)
    }
    Set::class.java -> {
      defaultValue?.let {
        getStringSet(key, it as Set<String>)
      } ?: getStringSet(key)
    }
    // Uncomment this to make the Kotlin intelliJ plugin explode
//    Enum::class.java -> {
//      getEnum(key, defaultValue!!, targetClass)
//    }
    else -> TODO("Unsupported type")
  } as Preference<T>
}

operator fun <T> Preference<T>.invoke() = get()
