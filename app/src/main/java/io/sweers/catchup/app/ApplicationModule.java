package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;
import dagger.Binds;
import dagger.Module;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;

@Module
public abstract class ApplicationModule {

  @Binds @ApplicationContext
  public abstract Context provideApplicationContext(Application application);
}
