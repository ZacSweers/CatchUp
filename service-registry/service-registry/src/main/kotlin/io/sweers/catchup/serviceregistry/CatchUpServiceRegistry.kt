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
package io.sweers.catchup.serviceregistry

import com.squareup.anvil.annotations.compat.MergeModules
import dagger.multibindings.Multibinds
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceIndex
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaIndex

// TODO can't then contribute these up to hilt
//  until https://github.com/google/ksp/issues/438

@MergeModules(ServiceIndex::class)
interface CatchUpServiceRegistry {
  @Multibinds fun serviceIndexes(): Map<String, Service>
}

@MergeModules(ServiceMetaIndex::class)
interface CatchUpServiceMetaRegistry {
  @Multibinds fun serviceMetaIndexes(): Map<String, ServiceMeta>
}
