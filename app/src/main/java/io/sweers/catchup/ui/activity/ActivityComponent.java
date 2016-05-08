package io.sweers.catchup.ui.activity;

import com.squareup.moshi.Moshi;

import io.sweers.catchup.app.ApplicationComponent;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.injection.PerActivity;
import io.sweers.catchup.ui.base.ActionBarProvider;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

@PerActivity
@dagger.Component(
    modules = ActivityModule.class,
    dependencies = ApplicationComponent.class
)
public interface ActivityComponent {
  void inject(MainActivity activity);

  CustomTabActivityHelper customTab();

  OkHttpClient okhttpClient();

  ActionBarProvider actionBarProvider();

  Moshi moshi();

  RxJavaCallAdapterFactory rxJavaCallAdapterFactory();

  LinkManager linkManager();
}
