package io.sweers.catchup.ui.activity;

import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.ui.ViewContainer;

@Module
public abstract class UiModule {
  @Provides
  @PerActivity
  static ViewContainer provideViewContainer() {
    return ViewContainer.DEFAULT;
  }
}
