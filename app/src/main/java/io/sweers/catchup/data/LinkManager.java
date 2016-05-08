package io.sweers.catchup.data;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.f2prateek.rx.receivers.RxBroadcastReceiver;

import javax.inject.Inject;

import io.sweers.catchup.injection.PerActivity;
import io.sweers.catchup.rx.Confine;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import static io.sweers.catchup.rx.Transformers.doOnEmpty;

@PerActivity
public class LinkManager {

  private final MainActivity activity;
  private final CustomTabActivityHelper customTab;

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of user prefs
  private final ArrayMap<String, Boolean> dumbCache = new ArrayMap<>();

  @Inject public LinkManager(
      MainActivity activity,
      CustomTabActivityHelper customTab) {
    this.activity = activity;
    this.customTab = customTab;

    // Invalidate the cache when a new install/update happens
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
    RxBroadcastReceiver.create(activity, filter)
        .compose(Confine.to(activity))
        .subscribe(intent -> dumbCache.clear());
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
    return match >= IntentFilter.MATCH_CATEGORY_HOST
        && match <= IntentFilter.MATCH_CATEGORY_PATH;
  }

  public void openUrl(@NonNull String url) {
    openUrl(Uri.parse(url));
  }

  public void openUrl(@NonNull Uri uri) {
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    if (!dumbCache.containsKey(uri.getHost())) {
      queryAndOpen(uri, intent);
    } else if (dumbCache.get(uri.getHost())) {
      activity.startActivity(intent);
    } else {
      openCustomTab(uri);
    }
  }

  private void queryAndOpen(Uri uri, Intent intent) {
    PackageManager manager = activity.getPackageManager();
    Observable.defer(() -> Observable.from(manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)))
        .filter(resolveInfo -> isSpecificUriMatch(resolveInfo.match))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .compose(doOnEmpty((Action0) () -> {
          dumbCache.put(uri.getHost(), false);
          openCustomTab(uri);
        }))
        .compose(Confine.to(activity))
        .subscribe(count -> {
          dumbCache.put(uri.getHost(), true);
          activity.startActivity(intent);
        });
  }

  private void openCustomTab(@NonNull Uri uri) {
    customTab.openCustomTab(customTab.getCustomTabIntent().build(), uri);
  }
}
