package io.sweers.catchup.ui.controllers;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import butterknife.BindView;
import butterknife.Unbinder;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.support.RouterPagerAdapter;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.jakewharton.processphoenix.ProcessPhoenix;
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.sweers.catchup.P;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.ConductorInjection;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.injection.qualifiers.preferences.NavBarTheme;
import io.sweers.catchup.rx.PredicateConsumer;
import io.sweers.catchup.ui.Scrollable;
import io.sweers.catchup.ui.activity.SettingsActivity;
import io.sweers.catchup.ui.base.ButterKnifeController;
import io.sweers.catchup.util.ApiUtil;
import io.sweers.catchup.util.UiUtil;
import java.util.Arrays;
import javax.inject.Inject;

public class PagerController extends ButterKnifeController {

  private static final String PAGE_TAG = "PagerController.pageTag";
  private static final int[][] PAGE_DATA = new int[][] {
      {
          R.drawable.logo_hn, R.string.hacker_news, R.color.hackerNewsAccent
      }, {
      R.drawable.logo_reddit, R.string.reddit, R.color.redditAccent
  }, {
      R.drawable.logo_medium, R.string.medium, R.color.mediumAccent
  }, {
      R.drawable.logo_ph, R.string.product_hunt, R.color.productHuntAccent
  }, {
      R.drawable.logo_sd, R.string.slashdot, R.color.slashdotAccent
  }, {
      R.drawable.logo_dn, R.string.designer_news, R.color.designerNewsAccent
  }, {
      R.drawable.logo_dribbble, R.string.dribbble, R.color.dribbbleAccent
  }, {
      R.drawable.logo_github, R.string.github, R.color.githubAccent
  }
  };
  private final int[] resolvedColorCache = new int[PAGE_DATA.length];
  private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

  @Inject @NavBarTheme Lazy<Preference<Boolean>> themeNavigationBarPref;
  @BindView(R.id.tab_layout) TabLayout tabLayout;
  @BindView(R.id.view_pager) ViewPager viewPager;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbarlayout) AppBarLayout appBarLayout;
  private ValueAnimator statusBarColorAnimator = null;
  private boolean colorNavBar = false;
  private boolean tablayoutIsPinned = false;
  private RouterPagerAdapter pagerAdapter;

  public PagerController() {
    super();
    init();
  }

  public PagerController(Bundle args) {
    super(args);
    init();
  }

  private void init() {
    pagerAdapter = new RouterPagerAdapter(this) {
      @Override public void configureRouter(Router router, int position) {
        if (!router.hasRootController()) {
          Controller page;
          switch (position) {
            case 0:
              page = new HackerNewsController();
              break;
            case 1:
              page = new RedditController();
              break;
            case 2:
              page = new MediumController();
              break;
            case 3:
              page = new ProductHuntController();
              break;
            case 4:
              page = new SlashdotController();
              break;
            case 5:
              page = new DesignerNewsController();
              break;
            case 6:
              page = new DribbbleController();
              break;
            case 7:
              page = new GitHubController();
              break;
            default:
              page = new RedditController();
          }
          router.setRoot(RouterTransaction.with(page)
              .tag(PAGE_TAG));
        }
      }

      @Override public int getCount() {
        return PAGE_DATA.length;
      }

      @Override public CharSequence getPageTitle(int position) {
        return "";
      }
    };

    // Invalidate the color cache up front
    Arrays.fill(resolvedColorCache, R.color.no_color);
  }

  public static void setLightStatusBar(@NonNull View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      view.setSystemUiVisibility(flags);
    }
  }

  public static void clearLightStatusBar(@NonNull View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int flags = view.getSystemUiVisibility();
      flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      view.setSystemUiVisibility(flags);
    }
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_pager, container, false);
  }

  @Override protected Unbinder bind(@NonNull View view) {
    return new PagerController_ViewBinding(this, view);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    ConductorInjection.inject(this);

    @ColorInt int colorPrimaryDark =
        UiUtil.resolveAttribute(view.getContext(), R.attr.colorPrimaryDark);
    boolean isInNightMode = UiUtil.isInNightMode(view.getContext());
    if (!isInNightMode) {
      // Start with a light status bar in normal mode
      setLightStatusBar(appBarLayout);
    }
    RxAppBarLayout.offsetChanges(appBarLayout)
        .distinctUntilChanged()
        .doOnNext(new PredicateConsumer<Integer>() {
          @Override public boolean test(Integer verticalOffset) throws Exception {
            return verticalOffset == -toolbar.getHeight();
          }

          @Override public void acceptActual(Integer verticalOffset) throws Exception {
            if (statusBarColorAnimator != null) {
              statusBarColorAnimator.cancel();
            }
            tablayoutIsPinned = true;
            int newStatusColor =
                PagerController.this.getAndSaveColor(tabLayout.getSelectedTabPosition());
            statusBarColorAnimator = ValueAnimator.ofArgb(colorPrimaryDark, newStatusColor);
            statusBarColorAnimator.addUpdateListener(animation -> PagerController.this.getActivity()
                .getWindow()
                .setStatusBarColor((int) animation.getAnimatedValue()));
            statusBarColorAnimator.setDuration(200);
            statusBarColorAnimator.setInterpolator(new FastOutSlowInInterpolator());
            statusBarColorAnimator.start();
            clearLightStatusBar(appBarLayout);
          }
        })
        .doOnNext(new PredicateConsumer<Integer>() {
          @Override public void acceptActual(Integer verticalOffset) throws Exception {
            boolean wasPinned = tablayoutIsPinned;
            tablayoutIsPinned = false;
            if (wasPinned) {
              if (statusBarColorAnimator != null) {
                statusBarColorAnimator.cancel();
              }
              statusBarColorAnimator = ValueAnimator.ofArgb(PagerController.this.getActivity()
                  .getWindow()
                  .getStatusBarColor(), colorPrimaryDark);
              statusBarColorAnimator.addUpdateListener(animation -> PagerController.this.getActivity()
                  .getWindow()
                  .setStatusBarColor((int) animation.getAnimatedValue()));
              statusBarColorAnimator.setDuration(200);
              statusBarColorAnimator.setInterpolator(new DecelerateInterpolator());
              if (!isInNightMode) {
                setLightStatusBar(appBarLayout);
              }
              statusBarColorAnimator.start();
            }
          }

          @Override public boolean test(Integer verticalOffset) throws Exception {
            return verticalOffset != -toolbar.getHeight();
          }
        })
        .subscribe();
    toolbar.inflateMenu(R.menu.main);
    toolbar.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.toggle_daynight:
          P.daynightAuto.put(false)
              .commit();
          if (UiUtil.isInNightMode(getActivity())) {
            P.daynightNight.put(false)
                .commit();
          } else {
            P.daynightNight.put(true)
                .commit();
          }
          ProcessPhoenix.triggerRebirth(getActivity());
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
    if (ApiUtil.isL() && !UiUtil.isInNightMode(view.getContext())) {
      colorNavBar = themeNavigationBarPref.get()
          .get(); // ew
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
            getActivity().getWindow()
                .setNavigationBarColor(color);
          });
    }

    viewPager.setAdapter(pagerAdapter);
    tabLayout.setupWithViewPager(viewPager);

    // Set icons
    for (int i = 0; i < PAGE_DATA.length; i++) {
      int[] vals = PAGE_DATA[i];
      Drawable d = VectorDrawableCompat.create(getResources(), vals[0], null);
      tabLayout.getTabAt(i)
          .setIcon(d);
    }

    // Animate color changes
    // adapted from http://kubaspatny.github.io/2014/09/18/viewpager-background-transition/
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int color;
        if (position < (pagerAdapter.getCount() - 1) && position < (PAGE_DATA.length - 1)) {
          color = (Integer) argbEvaluator.evaluate(positionOffset,
              getAndSaveColor(position),
              getAndSaveColor(position + 1));
        } else {
          color = getAndSaveColor(PAGE_DATA.length - 1);
        }
        tabLayout.setBackgroundColor(color);
        if (tablayoutIsPinned) {
          getActivity().getWindow()
              .setStatusBarColor(color);
        }
        if (colorNavBar) {
          getActivity().getWindow()
              .setNavigationBarColor(color);
        }
      }

      @Override public void onPageSelected(int position) {
        toolbar.setTitle(PAGE_DATA[position][1]);
      }

      @Override public void onPageScrollStateChanged(int state) {
        // NO-OP.
      }
    });

    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override public void onTabSelected(TabLayout.Tab tab) {

      }

      @Override public void onTabUnselected(TabLayout.Tab tab) {

      }

      @Override public void onTabReselected(TabLayout.Tab tab) {
        Controller controller = pagerAdapter.getRouter(tab.getPosition())
            .getControllerWithTag(PAGE_TAG);
        if (controller instanceof Scrollable) {
          ((Scrollable) controller).onRequestScrollToTop();
          appBarLayout.setExpanded(true, true);
        }
      }
    });
  }

  @ColorInt private int getAndSaveColor(int position) {
    if (resolvedColorCache[position] == R.color.no_color) {
      resolvedColorCache[position] = ContextCompat.getColor(getActivity(), PAGE_DATA[position][2]);
    }
    return resolvedColorCache[position];
  }

  @Subcomponent
  public interface Component extends AndroidInjector<PagerController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<PagerController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Binds @IntoMap @ControllerKey(PagerController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindPagerControllerInjectorFactory(
        Component.Builder builder);

    @Provides @NavBarTheme
    static Preference<Boolean> provideThemeNavigationColorPreference(RxSharedPreferences rxSharedPreferences) {
      return rxSharedPreferences.getBoolean(P.themeNavigationBar.key,
          P.themeNavigationBar.defaultValue());
      // TODO revert to this when this is fixed: https://github.com/Flipboard/psync/issues/11
      //      return P.themeNavigationBar.rx();
    }
  }
}
