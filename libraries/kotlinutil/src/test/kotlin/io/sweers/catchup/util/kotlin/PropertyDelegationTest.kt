/*
 * Copyright (C) 2019. Zac Sweers
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
package io.sweers.catchup.util.kotlin

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class PropertyDelegationTest {
  @Test
  fun smokeTest() {
    assertThat(B().readOnlyProp).isEqualTo(A.readOnlyProp)
    assertThat(B().readWriteProp).isEqualTo(A.readWriteProp)
    A.readWriteProp = "newValue"
    assertThat(B().readWriteProp).isEqualTo(A.readWriteProp)
    assertThat(B().readWriteProp).isEqualTo("newValue")
  }
}

private object A {
  val readOnlyProp: String = "value"
  var readWriteProp: String = ""
}

private class B {
  val readOnlyProp: String by A::readOnlyProp
  var readWriteProp: String by A::readWriteProp
}

private class C {
  val readOnlyProp: String = "value"
  var readWriteProp: String = ""
}

private class D {
  val readOnlyProp: String by C::readOnlyProp
  var readWriteProp: String by C::readWriteProp
}

private operator fun <T, R> KMutableProperty1<T, R>.setValue(
  ref: D,
  property: KProperty<*>,
  value: R
) {
}

private operator fun <T, R> KProperty1<T, R>.getValue(ref: D, property: KProperty<*>): R {
}
