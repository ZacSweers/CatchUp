package io.sweers.catchup.data.smmry;

import com.squareup.moshi.Moshi;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

@Module
public class SmmryModule {

  @Provides @Singleton static SmmryService provideSmmryService(final Lazy<OkHttpClient> client,
      Moshi moshi,
      RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
    return new Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
        .callFactory(request -> client.get()
            .newCall(request))
        .addCallAdapterFactory(rxJavaCallAdapterFactory)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SmmryService.class);
  }
}
