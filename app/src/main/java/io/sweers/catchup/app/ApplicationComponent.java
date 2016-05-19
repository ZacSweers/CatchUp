package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.ui.debug.DebugView;
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
  void inject(DebugView debugView);

  Application application();

  @ApplicationContext
  Context context();

  LumberYard lumberYard();

  OkHttpClient okhttpClient();

  Moshi moshi();

  RxJavaCallAdapterFactory rxJavaCallAdapterFactory();

  RxSharedPreferences rxSharedPreferences();
}
