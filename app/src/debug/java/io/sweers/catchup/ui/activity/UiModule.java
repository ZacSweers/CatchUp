package io.sweers.catchup.ui.activity;

import dagger.Module;
import dagger.Provides;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.ui.DebugViewContainer;
import io.sweers.catchup.ui.ViewContainer;

@Module
public class UiModule {
  @Provides
  @PerActivity
  ViewContainer provideViewContainer(DebugViewContainer viewContainer) {
    return viewContainer;
  }
}
