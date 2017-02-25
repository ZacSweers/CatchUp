package io.sweers.catchup.app;

import android.app.Application;
import android.content.Context;
import dagger.Binds;
import dagger.Module;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import javax.inject.Singleton;

@Module
public abstract class ApplicationModule {

  @Binds @Singleton @ApplicationContext
  public abstract Context provideApplicationContext(Application application);
}
