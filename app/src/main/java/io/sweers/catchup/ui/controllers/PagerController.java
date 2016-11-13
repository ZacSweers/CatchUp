package io.sweers.catchup.ui.controllers;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.support.ControllerPagerAdapter;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;

import java.util.Arrays;

import javax.inject.Inject;

import butterknife.BindView;
import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.qualifiers.preferences.NavBarTheme;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.Scrollable;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.activity.SettingsActivity;
import io.sweers.catchup.ui.base.BaseController;
import io.sweers.catchup.util.ApiUtil;
import io.sweers.catchup.util.UiUtil;

public class PagerController extends BaseController {

  private static final int[][] PAGE_DATA = new int[][]{
      {
          R.drawable.logo_hn,
          R.string.hacker_news,
          R.color.hackerNewsAccent
      },
      {
          R.drawable.logo_reddit,
          R.string.reddit,
          R.color.redditAccent
      },
      {
          R.drawable.logo_medium,
          R.string.medium,
          R.color.mediumAccent
      },
      {
          R.drawable.ll_ph,
          R.string.product_hunt,
          R.color.productHuntAccent
      },
      {
          R.drawable.logo_sd,
          R.string.slashdot,
          R.color.slashdotAccent
      },
      {
          R.drawable.logo_dn,
          R.string.designer_news,
          R.color.designerNewsAccent
      },
      {
          R.drawable.logo_dribbble,
          R.string.dribbble,
          R.color.dribbbleAccent
      },
      {
          R.drawable.logo_github,
          R.string.github,
          R.color.redditAccent
      }
  };
  private final int[] resolvedColorCache = new int[PAGE_DATA.length];
  private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
  @Inject @NavBarTheme Lazy<Preference<Boolean>> themeNavigationBarPref;
  @BindView(R.id.tab_layout) TabLayout tabLayout;
  @BindView(R.id.view_pager) ViewPager viewPager;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbarlayout) AppBarLayout appBarLayout;
  private boolean colorNavBar = false;
  private ControllerPagerAdapter pagerAdapter;

  // Ew, but the only way to get the controller later
  // https://github.com/bluelinelabs/Conductor/issues/166
  private SparseArrayCompat<Controller> controllers = new SparseArrayCompat<>();

  public PagerController() {
    pagerAdapter = new ControllerPagerAdapter(this, true) {
      @Override
      public Object instantiateItem(ViewGroup container, int position) {
        Controller controller = (Controller) super.instantiateItem(container, position);
        controllers.put(position, controller);
        return controller;
      }

      @Override
      public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        controllers.remove(position);
      }

      @Override
      public Controller getItem(int position) {
        switch (position) {
          case 0:
            return new HackerNewsController();
          case 1:
            return new RedditController();
          case 2:
            return new MediumController();
          case 3:
            return new ProductHuntController();
          case 4:
            return new SlashdotController();
          case 5:
            return new DesignerNewsController();
          case 6:
            return new DribbbleController();
          case 7:
            return new GitHubController();
          default:
            return new RedditController();
        }
      }

      @Override
      public int getCount() {
        return PAGE_DATA.length;
      }

      @Override
      public CharSequence getPageTitle(int position) {
        return "";
      }
    };

    // Invalidate the color cache up front
    Arrays.fill(resolvedColorCache, R.color.no_color);
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_pager, container, false);
  }

  @Override
  protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);

    // TODO Must be a sooner place to inject this
    createComponent().inject(this);

    toolbar.inflateMenu(R.menu.main);
    toolbar.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.toggle_daynight:
          P.daynightAuto.put(false).commit();
          if (UiUtil.isInNightMode(getActivity())) {
            P.daynightNight.put(false).commit();
          } else {
            P.daynightNight.put(true).commit();
          }
          getActivity().recreate();
          return true;
        case R.id.settings:
          startActivity(new Intent(getActivity(), SettingsActivity.class));
          return true;
      }
      return false;
    });

    // Initial title
    toolbar.setTitle(getResources().getString(PAGE_DATA[0][1]));

    // Set the initial color
    @ColorInt int initialColor = getAndSaveColor(0);
    tabLayout.setBackgroundColor(initialColor);
    if (ApiUtil.isL()
        && !UiUtil.isInNightMode(view.getContext())) {
      colorNavBar = themeNavigationBarPref.get().get(); // ew
      themeNavigationBarPref.get()
          .asObservable()
          .distinctUntilChanged()
          .subscribe(b -> {
            colorNavBar = b;
            int color;
            if (b) {
              color = getAndSaveColor(viewPager.getCurrentItem());
            } else {
              color = Color.BLACK;
            }
            getActivity().getWindow().setNavigationBarColor(color);
          });
    }

    viewPager.setAdapter(pagerAdapter);
    tabLayout.setupWithViewPager(viewPager);

    // Set icons
    for (int i = 0; i < PAGE_DATA.length; i++) {
      int[] vals = PAGE_DATA[i];
      Drawable d = VectorDrawableCompat.create(getResources(), vals[0], null);
      tabLayout.getTabAt(i).setIcon(d);
    }

    // Animate color changes
    // adapted from http://kubaspatny.github.io/2014/09/18/viewpager-background-transition/
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int color;
        if (position < (pagerAdapter.getCount() - 1) && position < (PAGE_DATA.length - 1)) {
          color = (Integer) argbEvaluator.evaluate(positionOffset, getAndSaveColor(position), getAndSaveColor(position + 1));
        } else {
          color = getAndSaveColor(PAGE_DATA.length - 1);
        }
        tabLayout.setBackgroundColor(color);
        if (colorNavBar) {
          getActivity().getWindow().setNavigationBarColor(color);
        }
      }

      @Override
      public void onPageSelected(int position) {
        toolbar.setTitle(PAGE_DATA[position][1]);
      }

      @Override
      public void onPageScrollStateChanged(int state) {
        // NO-OP.
      }
    });

    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {

      }

      @Override
      public void onTabUnselected(TabLayout.Tab tab) {

      }

      @Override
      public void onTabReselected(TabLayout.Tab tab) {
        Controller controller = controllers.get(tab.getPosition());
        if (controller instanceof Scrollable) {
          ((Scrollable) controller).onRequestScrollToTop();
          appBarLayout.setExpanded(true, true);
        }
      }
    });
  }

  @ColorInt
  private int getAndSaveColor(int position) {
    if (resolvedColorCache[position] == R.color.no_color) {
      resolvedColorCache[position] = ContextCompat.getColor(getActivity(), PAGE_DATA[position][2]);
    }
    return resolvedColorCache[position];
  }

  protected Component createComponent() {
    return DaggerPagerController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  interface Component {
    void inject(PagerController pagerController);
  }

  @dagger.Module
  static class Module {

    @Provides
    @PerController
    @NavBarTheme
    Preference<Boolean> provideThemeNavigationColorPreference(RxSharedPreferences rxSharedPreferences) {
      return rxSharedPreferences.getBoolean(P.themeNavigationBar.key, P.themeNavigationBar.defaultValue());
      // TODO revert to this when this is fixed: https://github.com/Flipboard/psync/issues/11
//      return P.themeNavigationBar.rx();
    }

  }
}
