package io.sweers.catchup.app;

import android.app.Application;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.LumberYard;
import io.sweers.catchup.data.VariantDataModule;
import io.sweers.catchup.data.smmry.SmmryModule;
import io.sweers.catchup.injection.ConductorInjectionModule;
import io.sweers.catchup.ui.activity.ActivityModule;
import okhttp3.OkHttpClient;

@Component(modules = {
    ActivityModule.class, AndroidInjectionModule.class, ApplicationModule.class,
    ConductorInjectionModule.class, DataModule.class, SmmryModule.class, VariantDataModule.class
})
public interface ApplicationComponent {

  void inject(CatchUpApplication application);

  LumberYard lumberYard();

  Application application();

  OkHttpClient okHttpClient();

  @Component.Builder
  interface Builder {
    ApplicationComponent build();

    @BindsInstance Builder application(Application application);
  }
}
