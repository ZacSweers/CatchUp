package catchup.summarizer

import catchup.appconfig.AppConfig
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.libraries.retrofitconverters.delegatingCallFactory
import catchup.sqldelight.SqlDriverFactory
import catchup.util.network.AuthInterceptor
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@Qualifier private annotation class InternalApi

@ContributesTo(AppScope::class)
@Module
object SummarizerModule {

  @Provides
  @SingleIn(AppScope::class)
  fun provideSummarizerDatabase(factory: SqlDriverFactory): SummarizationsDatabase =
    SummarizationsDatabase(factory.create(SummarizationsDatabase.Schema, "summarizations.db"))

  @Provides
  @InternalApi
  fun provideOpenAiOkHttpClient(client: OkHttpClient): OkHttpClient {
    return client
      .newBuilder()
      .addInterceptor(AuthInterceptor("Bearer", BuildConfig.OPEN_AI_KEY))
      .build()
  }

  @Provides
  fun chatGptApi(
    @InternalApi client: Lazy<OkHttpClient>,
    moshi: Moshi,
    appConfig: AppConfig,
  ): ChatGptApi =
    Retrofit.Builder()
      .baseUrl("https://api.openai.com")
      .delegatingCallFactory(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .validateEagerly(appConfig.isDebug)
      .build()
      .create()
}
