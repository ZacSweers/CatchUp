/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.medium

import com.google.auto.value.AutoValue
import com.ryanharter.auto.value.moshi.MoshiAdapterFactory
import com.squareup.moshi.JsonAdapter
import io.sweers.inspector.Validator
import io.sweers.inspector.factorycompiler.InspectorFactory

object MediumModels {
  fun createMoshiAdapterFactory(): JsonAdapter.Factory = AutoValueMoshi_MediumAdapterFactory()

  fun createValidatorFactory(): Validator.Factory = InspectorFactory_MediumValidatorFactory()
}

@MoshiAdapterFactory(nullSafe = true)
abstract class MediumAdapterFactory : JsonAdapter.Factory

@InspectorFactory(include = [AutoValue::class])
abstract class MediumValidatorFactory : Validator.Factory
