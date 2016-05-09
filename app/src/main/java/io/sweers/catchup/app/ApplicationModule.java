package io.sweers.catchup.app;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.injection.ApplicationContext;

@Module
public class ApplicationModule {

  private CatchUpApplication application;

  public ApplicationModule(@NonNull CatchUpApplication application) {
    this.application = application;
  }

  @Provides
  @Singleton
  @ApplicationContext
  public Context provideApplicationContext() {
    return application.getApplicationContext();
  }
}
