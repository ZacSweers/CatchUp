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
package io.sweers.catchup.data

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.FloatColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import dev.zacsweers.catchup.service.CatchUpDatabase
import dev.zacsweers.catchup.service.CatchUpDbItem
import io.sweers.catchup.util.injection.qualifiers.ApplicationContext
import kotlinx.datetime.Instant

@ContributesTo(AppScope::class)
@Module
object CatchUpDatabaseModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideCatchUpDatabase(@ApplicationContext context: Context): CatchUpDatabase =
    CatchUpDatabase(
      AndroidSqliteDriver(CatchUpDatabase.Schema, context, "catchUpItems.db"),
      CatchUpDbItem.Adapter(
        InstantColumnAdapter,
        IntColumnAdapter,
        IntColumnAdapter,
        IntColumnAdapter,
        IntColumnAdapter,
        FloatColumnAdapter,
        IntColumnAdapter,
      )
    )
}

object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

fun CatchUpDatabase.lastUpdated(serviceId: String): Long? {
  return serviceQueries.lastOperation(serviceId).executeAsOneOrNull()?.timestamp
}
