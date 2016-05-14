package io.sweers.catchup.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.jakewharton.processphoenix.ProcessPhoenix;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.app.CatchUpApplication;
import io.sweers.catchup.ui.ViewContainer;
import io.sweers.catchup.ui.base.ActionBarProvider;
import io.sweers.catchup.ui.base.BaseActivity;
import io.sweers.catchup.ui.controllers.PagerController;
import io.sweers.catchup.util.UiUtil;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;

public class MainActivity extends BaseActivity implements ActionBarProvider {

  @Inject CustomTabActivityHelper customTab;
  @Inject ViewContainer viewContainer;

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.controller_container) ViewGroup container;

  private Router router;
  private ActivityComponent component;
  private Unbinder unbinder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    component = createComponent();
    component.inject(this);
    ViewGroup viewGroup = viewContainer.forActivity(this);
    getLayoutInflater().inflate(R.layout.activity_main, viewGroup);

    unbinder = ButterKnife.bind(this);

    setSupportActionBar(toolbar);

    router = Conductor.attachRouter(this, container, savedInstanceState);
    if (!router.hasRootController()) {
      router.setRoot(new PagerController());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.toggle_daynight:
        P.daynightAuto.put(false).commit();
        if (UiUtil.isInNightMode(this)) {
          P.daynightNight.put(false).commit();
        } else {
          P.daynightNight.put(true).commit();
        }
        // TODO Use recreate() here after conductor 2.0 and not needing to retain views on detach
        ProcessPhoenix.triggerRebirth(this);
        return true;
      case R.id.settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  protected ActivityComponent createComponent() {
    return DaggerActivityComponent.builder()
        .applicationComponent(CatchUpApplication.component())
        .activityModule(new ActivityModule(this))
        .uiModule(new UiModule())
        .build();
  }

  @Override
  protected void onStart() {
    super.onStart();
    customTab.bindCustomTabsService(this);
  }

  @Override
  protected void onStop() {
    customTab.unbindCustomTabsService(this);
    super.onStop();
  }

  @Override
  public void onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onDestroy() {
    customTab.setConnectionCallback(null);
    if (unbinder != null) {
      unbinder.unbind();
    }
    super.onDestroy();
  }

  @NonNull
  public ActivityComponent getComponent() {
    return component;
  }

}
