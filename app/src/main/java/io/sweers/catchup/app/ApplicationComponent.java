package io.sweers.catchup.app;

import javax.inject.Singleton;

import dagger.Component;
import io.sweers.catchup.network.NetworkModule;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

@Singleton
@Component(
    modules = {
        ApplicationModule.class,
        NetworkModule.class
    }
)
public interface ApplicationComponent {
  void inject(CatchUpApplication application);

  OkHttpClient okhttpClient();

  MoshiConverterFactory moshiConverterFactory();

  RxJavaCallAdapterFactory rxJavaCallAdapterFactory();
}
