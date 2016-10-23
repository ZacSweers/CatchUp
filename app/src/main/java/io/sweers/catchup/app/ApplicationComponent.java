package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;

@Singleton
@Component(
    modules = {
        ApplicationModule.class,
        DataModule.class
    }
)
public interface ApplicationComponent {
  void inject(CatchUpApplication application);

  Application application();

  @ApplicationContext
  Context context();

  LumberYard lumberYard();

  OkHttpClient okhttpClient();

  Moshi moshi();

  RxJava2CallAdapterFactory rxJavaCallAdapterFactory();

  RxSharedPreferences rxSharedPreferences();
}
