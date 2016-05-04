package io.sweers.catchup.app;

import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import io.sweers.catchup.data.DataModule;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

@Singleton
@Component(
    modules = {
        ApplicationModule.class,
        DataModule.class
    }
)
public interface ApplicationComponent {
  void inject(CatchUpApplication application);

  OkHttpClient okhttpClient();

  Moshi moshi();

  RxJavaCallAdapterFactory rxJavaCallAdapterFactory();
}
