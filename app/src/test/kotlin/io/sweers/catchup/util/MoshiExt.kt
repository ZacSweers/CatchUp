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
package io.sweers.catchup.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
inline fun <reified T> Moshi.adapter(): JsonAdapter<T> = adapter(typeOf<T>())

inline fun <reified T> Moshi.adapter(ktype: KType): JsonAdapter<T> {
  val (type, isMarkedNullable) = ktype.asType()
  val adapter = adapter<T>(type)
  // It would be nice if we could check if the adapter was already nullsafe!
  return if (isMarkedNullable) {
    adapter.nullSafe()
  } else {
    adapter.nonNull()
  }
}

fun KType.asType(): TypeWithNullability {
  val classifier = this.classifier
  if (classifier is KTypeParameter || classifier == null || classifier !is KClass<*>) {
    throw IllegalArgumentException("Cannot build TypeName for $this")
  }
  if (arguments.isEmpty()) {
    return TypeWithNullability(classifier.javaObjectType, isMarkedNullable)
  }
  if (classifier.java.isArray) {
    return TypeWithNullability(Types.arrayOf(arguments[1].asType()), isMarkedNullable)
  }
  val enclosingClass = classifier.java.enclosingClass
  val finalArgs = arguments.take(classifier.typeParameters.size).map(
      KTypeProjection::asType).toTypedArray()
  val finalType = if (enclosingClass == null) {
    Types.newParameterizedType(classifier.java, *finalArgs)
  } else {
    Types.newParameterizedTypeWithOwner(enclosingClass, classifier.java, *finalArgs)
  }
  return TypeWithNullability(finalType, isMarkedNullable)
}

private val STAR: Type = Types.subtypeOf(Any::class.java)

fun KTypeProjection.asType(): Type {
  val (paramVariance, paramType) = this
  val typeName = paramType?.asType() ?: return STAR
  return when (paramVariance) {
    null -> STAR
    KVariance.INVARIANT -> typeName.type
    KVariance.IN -> Types.subtypeOf(typeName.type)
    KVariance.OUT -> Types.supertypeOf(typeName.type)
  }
}

class TypeWithNullability(val type: Type, val isMarkedNullable: Boolean) {
  operator fun component1(): Type = type
  operator fun component2(): Boolean = isMarkedNullable
}
