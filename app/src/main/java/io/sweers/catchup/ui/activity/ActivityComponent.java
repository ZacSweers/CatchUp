package io.sweers.catchup.ui.activity;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.ui.controllers.DesignerNewsController;
import io.sweers.catchup.ui.controllers.DribbbleController;
import io.sweers.catchup.ui.controllers.GitHubController;
import io.sweers.catchup.ui.controllers.HackerNewsController;
import io.sweers.catchup.ui.controllers.MediumController;
import io.sweers.catchup.ui.controllers.PagerController;
import io.sweers.catchup.ui.controllers.ProductHuntController;
import io.sweers.catchup.ui.controllers.RedditController;
import io.sweers.catchup.ui.controllers.SlashdotController;

@PerActivity
@Subcomponent(modules = {
    ActivityModule.class,
    UiModule.class,
    PagerController.Module.class,
    HackerNewsController.Module.class,
    RedditController.Module.class,
    MediumController.Module.class,
    ProductHuntController.Module.class,
    SlashdotController.Module.class,
    DesignerNewsController.Module.class,
    DribbbleController.Module.class,
    GitHubController.Module.class,
})
public interface ActivityComponent extends AndroidInjector<MainActivity> {

  @Subcomponent.Builder
  abstract class Builder extends AndroidInjector.Builder<MainActivity> {

  }
}
