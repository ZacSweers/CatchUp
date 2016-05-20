package io.sweers.catchup.ui.activity;

import android.content.Context;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.moshi.Moshi;

import dagger.Component;
import io.sweers.catchup.app.ApplicationComponent;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

@PerActivity
@Component(
    modules = {
        ActivityModule.class,
        UiModule.class
    },
    dependencies = ApplicationComponent.class
)
public interface ActivityComponent {
  void inject(MainActivity activity);

  @ApplicationContext
  Context context();

  CustomTabActivityHelper customTab();

  LinkManager linkManager();

  OkHttpClient okhttpClient();

  Moshi moshi();

  RxJavaCallAdapterFactory rxJavaCallAdapterFactory();

  RxSharedPreferences rxSharedPreferences();

}
