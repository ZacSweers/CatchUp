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

package io.sweers.catchup.serviceregistry

import dagger.Module
import dagger.multibindings.Multibinds
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceRegistry

@Module(includes = [CatchUpServiceMetaRegistry::class, ResolvedCatchUpServiceRegistry::class])
@ServiceRegistry
abstract class CatchUpServiceRegistry {
  @Multibinds
  abstract fun services(): Map<String, Service>
}


@Module(includes = [ResolvedCatchUpServiceMetaRegistry::class])
@Meta
@ServiceRegistry
abstract class CatchUpServiceMetaRegistry {
  @Multibinds
  abstract fun serviceMetas(): Map<String, ServiceMeta>
}
