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
package catchup.service.api

sealed interface Service {
  val supportsFakeData: Boolean
    get() = false

  fun meta(): ServiceMeta

  suspend fun fetch(request: DataRequest): DataResult

  suspend fun fetchDetail(item: CatchUpItem, detailKey: String): Detail {
    throw NotImplementedError()
  }
}
