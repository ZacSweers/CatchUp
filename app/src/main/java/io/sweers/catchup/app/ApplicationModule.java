package io.sweers.catchup.app;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

  private CatchUpApplication application;

  public ApplicationModule(@NonNull CatchUpApplication application) {
    this.application = application;
  }

  @Provides @Singleton
  public CatchUpApplication provideApplication() {
    return application;
  }

}
