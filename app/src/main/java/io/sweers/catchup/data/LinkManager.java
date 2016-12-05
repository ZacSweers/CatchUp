package io.sweers.catchup.data;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.widget.Toast;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.receivers.RxBroadcastReceiver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.rx.Transformers;
import io.sweers.catchup.rx.boundlifecycle.observers.BoundObservers;
import io.sweers.catchup.rx.boundlifecycle.observers.Disposables;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import javax.inject.Inject;
import rx.functions.Action1;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static io.sweers.catchup.rx.Transformers.doOnEmpty;

@PerActivity public class LinkManager implements Action1<Pair<String, Integer>> {

  private final MainActivity activity;
  private final CustomTabActivityHelper customTab;
  @SmartLinking private final Preference<Boolean> globalSmartLinkingPref;

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of per-service prefs
  private final ArrayMap<String, Boolean> dumbCache = new ArrayMap<>();

  @Inject
  public LinkManager(MainActivity activity,
      CustomTabActivityHelper customTab,
      @SmartLinking Preference<Boolean> globalSmartLinkingPref) {
    this.activity = activity;
    this.customTab = customTab;
    this.globalSmartLinkingPref = globalSmartLinkingPref;

    // Invalidate the cache when a new install/update happens or prefs changed
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
    Observable.merge(toV2Observable(RxBroadcastReceiver.create(activity, filter)),
        toV2Observable(globalSmartLinkingPref.asObservable()))
        .subscribe(Disposables.forObservable(activity)
            .around(o -> dumbCache.clear()));
  }

  /**
   * Neat little helper method to check if a {@link android.content.pm.ResolveInfo#match} is for a
   * specific match or not. This is useful for checking if the ResolveInfo instance itself
   * is a browser or not.
   *
   * @param match the match int as provided by the ResolveInfo result
   * @return {@code true} if it's a specific Uri match, {@code false} if not.
   */
  private static boolean isSpecificUriMatch(int match) {
    match = match & IntentFilter.MATCH_CATEGORY_MASK;
    return match >= IntentFilter.MATCH_CATEGORY_HOST && match <= IntentFilter.MATCH_CATEGORY_PATH;
  }

  public void openUrl(@NonNull String url, @ColorInt int accentColor) {
    if (TextUtils.isEmpty(url)) {
      Toast.makeText(activity, R.string.error_no_url, Toast.LENGTH_SHORT)
          .show();
      return;
    }
    openUrl(Uri.parse(url), accentColor);
  }

  public void openUrl(@NonNull Uri uri, @ColorInt int accentColor) {
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    if (!globalSmartLinkingPref.get()) {
      openCustomTab(uri, accentColor);
      return;
    }

    if (!dumbCache.containsKey(uri.getHost())) {
      queryAndOpen(uri, intent, accentColor);
    } else if (dumbCache.get(uri.getHost())) {
      activity.startActivity(intent);
    } else {
      openCustomTab(uri, accentColor);
    }
  }

  private void queryAndOpen(Uri uri, Intent intent, @ColorInt int accentColor) {
    PackageManager manager = activity.getPackageManager();
    Observable.defer(() -> Observable.fromIterable(manager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY)))
        .filter(resolveInfo -> isSpecificUriMatch(resolveInfo.match))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .compose(Transformers.delayedMessage(activity.findViewById(android.R.id.content),
            "Resolving"))
        .compose(doOnEmpty(() -> {
          dumbCache.put(uri.getHost(), false);
          openCustomTab(uri, accentColor);
        }))
        .subscribe(Disposables.forObservable(activity)
            .around(o -> {
              dumbCache.put(uri.getHost(), true);
              activity.startActivity(intent);
            }));
  }

  private void openCustomTab(@NonNull Uri uri, @ColorInt int accentColor) {
    customTab.openCustomTab(customTab.getCustomTabIntent()
        .setStartAnimations(activity, R.anim.slide_up, R.anim.inset)
        .setExitAnimations(activity, R.anim.outset, R.anim.slide_down)
        .setToolbarColor(accentColor)
        .build(), uri);
  }

  @Override
  public void call(Pair<String, Integer> pair) {
    openUrl(pair.first, pair.second);
  }
}
