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
@file:Suppress("NOTHING_TO_INLINE")

package io.sweers.catchup.util.kotlin

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * An extension to delegate a read-only property of type [T] to another property.
 *
 * This extension allows for delegating on property to another. E.g. `val foo by SomeClass:otherFoo`
 */
inline operator fun <T> KProperty0<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()

/**
 * An extension to delegate a read-write property of type [T] to another read-write property.
 *
 * This extension allows for delegating on property to another. E.g. `var foo by SomeClass:otherFoo`
 */
inline operator fun <T> KMutableProperty0<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

interface InstanceProperty<T> {
  val instance: T
}

inline fun <T, R> instanceProperty(instance: R, property: KProperty1<T, R>) = object : InstanceProperty<R> {
  override val instance: R = instance
}

// TODO binding to an instant property
// /**
// * An extension to delegate a read-only property of type [T] to another property.
// *
// * This extension allows for delegating on property to another. E.g. `val foo by SomeClass:otherFoo`
// */
// inline operator fun <T, R> KProperty1<T, R>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
//
// /**
// * An extension to delegate a read-write property of type [T] to another read-write property.
// *
// * This extension allows for delegating on property to another. E.g. `var foo by SomeClass:otherFoo`
// */
// inline operator fun <T, R> KMutableProperty1<T, R>.setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
