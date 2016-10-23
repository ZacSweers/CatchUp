package io.sweers.catchup.ui.activity;

import android.content.Context;

import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.Moshi;

import dagger.Component;
import io.sweers.catchup.app.ApplicationComponent;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;

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

  RxJava2CallAdapterFactory rxJavaCallAdapterFactory();

  RxSharedPreferences rxSharedPreferences();

}
