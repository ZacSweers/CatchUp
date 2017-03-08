package io.sweers.catchup.app;

import android.app.Application;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.data.smmry.SmmryModule;
import io.sweers.catchup.injection.ConductorInjectionModule;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;

@Singleton
@Component(modules = {
    ApplicationModule.class,
    DataModule.class,
    SmmryModule.class,
    AndroidInjectionModule.class,
    ConductorInjectionModule.class
})
public interface ApplicationComponent {

  void inject(CatchUpApplication application);

  LumberYard lumberYard();
  Application application();
  OkHttpClient okHttpClient();

  @Component.Builder
  interface Builder {
    ApplicationComponent build();

    Builder dataModule(DataModule dataModule);

    @BindsInstance Builder application(Application application);
  }
}
