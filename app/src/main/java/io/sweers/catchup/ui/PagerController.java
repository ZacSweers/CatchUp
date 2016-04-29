package io.sweers.catchup.ui;

import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.support.ControllerPagerAdapter;
import com.jakewharton.rxbinding.support.v4.view.RxViewPager;

import javax.inject.Inject;

import butterknife.Bind;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.ActionBarProvider;
import io.sweers.catchup.ui.base.BaseController;

public class PagerController extends BaseController {

//  private static final int[] PAGE_COLORS = new int[]{
//      R.color.green_300,
//      R.color.cyan_300,
//      R.color.deep_purple_300,
//      R.color.lime_300,
//      R.color.red_300
//  };

  private static final String[][] PAGE_NAMES = new String[][]{
      {"HN", "Hacker News"},
      {"R", "Reddit"},
      {"PH", "Product Hunt"},
      {"SD", "SlashDot"},
      {"DN", "Designer News"},
      {"DR", "Dribbble"},
      {"GH", "GitHub"},
      {"M", "Medium"},
      {"RD", "Readability"}
  };

  @Inject ActionBarProvider actionBarProvider;

  @Bind(R.id.tab_layout) TabLayout tabLayout;
  @Bind(R.id.view_pager) ViewPager viewPager;

  private ControllerPagerAdapter pagerAdapter;

  public PagerController() {
    pagerAdapter = new ControllerPagerAdapter(this) {
      @Override
      public Controller getItem(int position) {
        switch (position) {
          case 0:
            return new HackerNewsController();
          case 1:
            return new RedditController();
          default:
            return new HackerNewsController();
        }
      }

      @Override
      public int getCount() {
        return PAGE_NAMES.length;
      }

      @Override
      public CharSequence getPageTitle(int position) {
        return PAGE_NAMES[position][0];
      }
    };
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_pager, container, false);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);

    // TODO Must be a sooner place to inject this
    createComponent().inject(this);

    viewPager.setAdapter(pagerAdapter);
    tabLayout.setupWithViewPager(viewPager);

    RxViewPager.pageSelections(viewPager)
        .subscribe(position -> {
          actionBarProvider.getSupportActionBar().setTitle(PAGE_NAMES[position][1]);
        });
  }

  protected Component createComponent() {
    return DaggerPagerController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build();
  }

  @PerController
  @dagger.Component(
      dependencies = ActivityComponent.class
  )
  interface Component {
    void inject(PagerController pagerController);
  }
}
