/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.activity;

import android.os.Bundle;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.ui.ViewContainer;
import io.sweers.catchup.ui.base.BaseActivity;
import io.sweers.catchup.ui.controllers.PagerController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import javax.inject.Inject;

public class MainActivity extends BaseActivity {

  @Inject CustomTabActivityHelper customTab;
  @Inject ViewContainer viewContainer;
  @Inject LinkManager linkManager;

  @BindView(R.id.controller_container) ViewGroup container;

  private Router router;
  private Unbinder unbinder;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ViewGroup viewGroup = viewContainer.forActivity(this);
    getLayoutInflater().inflate(R.layout.activity_main, viewGroup);

    unbinder = ButterKnife.bind(this);

    router = Conductor.attachRouter(this, container, savedInstanceState);
    if (!router.hasRootController()) {
      router.setRoot(RouterTransaction.with(new PagerController()));
    }
  }

  @Override protected void onStart() {
    super.onStart();
    customTab.bindCustomTabsService(this);
    linkManager.connect(this);
  }

  @Override protected void onStop() {
    customTab.unbindCustomTabsService(this);
    super.onStop();
  }

  @Override public void onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed();
    }
  }

  @Override protected void onDestroy() {
    customTab.setConnectionCallback(null);
    if (unbinder != null) {
      unbinder.unbind();
    }
    super.onDestroy();
  }
}
