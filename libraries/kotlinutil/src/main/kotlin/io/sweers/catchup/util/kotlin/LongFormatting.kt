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

package io.sweers.catchup.util.kotlin

import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.TreeMap

private val SUFFIXES = TreeMap<Long, String>().apply {
  put(1_000L, "k")
  put(1_000_000L, "M")
  put(1_000_000_000L, "G")
  put(1_000_000_000_000L, "T")
  put(1_000_000_000_000_000L, "P")
  put(1_000_000_000_000_000_000L, "E")
}

fun Long.format(): String {
  var shortened = shorten()
  if (!shortened.isEmpty() && !Character.isDigit(shortened.substring(shortened.length - 1)[0])) {
    shortened = shortened.replace('.',
        DecimalFormatSymbols(Locale.getDefault()).decimalSeparator)
  }
  return shortened
}

fun Long.shorten(): String {
  //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
  if (this == java.lang.Long.MIN_VALUE) {
    return (java.lang.Long.MIN_VALUE + 1).shorten()
  }
  if (this < 0) {
    return "-" + (-this).shorten()
  }
  if (this < 1000) {
    return java.lang.Long.toString(this) //deal with easy case
  }

  val e = SUFFIXES.floorEntry(this)
  val divideBy = e.key
  val suffix = e.value

  val truncated = this / (divideBy!! / 10) //the number part of the output times 10
  val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
  return if (hasDecimal) (truncated / 10.0).toString() + suffix else (truncated / 10).toString() + suffix
}
