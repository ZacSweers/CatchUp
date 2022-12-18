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
package io.sweers.catchup.service.producthunt

import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoMap
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.libraries.retrofitconverters.delegatingCallFactory
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.DataRequest
import io.sweers.catchup.service.api.DataResult
import io.sweers.catchup.service.api.Mark.Companion.createCommentMark
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.ServiceKey
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.service.api.ServiceMetaKey
import io.sweers.catchup.service.api.TextService
import io.sweers.catchup.util.data.adapters.ISO8601InstantAdapter
import io.sweers.catchup.util.network.AuthInterceptor
import javax.inject.Inject
import javax.inject.Qualifier
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier private annotation class InternalApi

private const val SERVICE_KEY = "ph"

@ServiceKey(SERVICE_KEY)
@ContributesMultibinding(AppScope::class, boundType = Service::class)
class ProductHuntService
@Inject
constructor(@InternalApi private val serviceMeta: ServiceMeta, private val api: ProductHuntApi) :
  TextService {

  override fun meta() = serviceMeta

  override suspend fun fetch(request: DataRequest): DataResult {
    val page = request.pageKey!!.toInt()
    return api
      .getPosts(page)
      .mapIndexed { index, it ->
        with(it) {
          CatchUpItem(
            id = id,
            title = name,
            score = "▲" to votesCount,
            timestamp = createdAt,
            author = user.name,
            tag = firstTopic,
            itemClickUrl = redirectUrl,
            mark = createCommentMark(count = commentsCount, clickUrl = discussionUrl),
            indexInResponse = index + request.pageOffset,
            serviceId = meta().id,
          )
        }
      }
      .let { DataResult(it, (page + 1).toString()) }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class ProductHuntMetaModule {

  @IntoMap
  @ServiceMetaKey(SERVICE_KEY)
  @Binds
  internal abstract fun productHuntServiceMeta(@InternalApi meta: ServiceMeta): ServiceMeta

  companion object {

    @InternalApi
    @Provides
    @Reusable
    internal fun provideProductHuntServiceMeta(): ServiceMeta =
      ServiceMeta(
        SERVICE_KEY,
        R.string.ph,
        R.color.phAccent,
        R.drawable.logo_ph,
        pagesAreNumeric = true,
        firstPageKey = 0,
        enabled =
          BuildConfig.PRODUCT_HUNT_DEVELOPER_TOKEN.run { !isNullOrEmpty() && !equals("null") }
      )
  }
}

@ContributesTo(AppScope::class)
@Module(includes = [ProductHuntMetaModule::class])
object ProductHuntModule {

  @Provides
  @InternalApi
  internal fun provideProductHuntOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor(AuthInterceptor("Bearer", BuildConfig.PRODUCT_HUNT_DEVELOPER_TOKEN))
      .build()
  }

  @Provides
  @InternalApi
  internal fun provideProductHuntMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder().add(Instant::class.java, ISO8601InstantAdapter()).build()
  }

  @Provides
  internal fun provideProductHuntService(
    @InternalApi client: Lazy<OkHttpClient>,
    @InternalApi moshi: Moshi,
    rxJavaCallAdapterFactory: RxJava3CallAdapterFactory,
    appConfig: AppConfig
  ): ProductHuntApi {
    return Retrofit.Builder()
      .baseUrl(ProductHuntApi.ENDPOINT)
      .delegatingCallFactory(client)
      .addCallAdapterFactory(rxJavaCallAdapterFactory)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create(ProductHuntApi::class.java)
  }
}
