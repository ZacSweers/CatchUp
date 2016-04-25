package io.sweers.catchup.app;

import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import io.sweers.catchup.network.NetworkModule;
import okhttp3.OkHttpClient;

@Singleton
@Component(
    modules = {
        ApplicationModule.class,
        NetworkModule.class
    }
)
public interface ApplicationComponent {
  void inject(CatchUpApplication application);

  Moshi moshi();

  OkHttpClient okhttpClient();
}
