package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.moshi.Moshi;
import dagger.BindsInstance;
import dagger.Component;
import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.data.smmry.SmmryModule;
import io.sweers.catchup.data.smmry.SmmryService;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

@Singleton
@Component(modules = {
    ApplicationModule.class, DataModule.class, SmmryModule.class
})
public interface ApplicationComponent {
  void inject(CatchUpApplication application);

  Application application();

  @ApplicationContext Context context();

  LumberYard lumberYard();

  OkHttpClient okhttpClient();

  Moshi moshi();

  RxJava2CallAdapterFactory rxJavaCallAdapterFactory();

  RxSharedPreferences rxSharedPreferences();

  SmmryService smmryService();

  @Component.Builder
  interface Builder {
    ApplicationComponent build();

    Builder dataModule(DataModule dataModule);

    @BindsInstance Builder application(Application application);
  }
}
