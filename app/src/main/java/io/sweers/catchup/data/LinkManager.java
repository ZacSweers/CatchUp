package io.sweers.catchup.data;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.widget.Toast;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.receivers.RxBroadcastReceiver;
import com.uber.autodispose.ObservableScoper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.qualifiers.preferences.SmartLinking;
import io.sweers.catchup.injection.scopes.PerActivity;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static io.sweers.catchup.rx.Transformers.doOnEmpty;

@PerActivity
public final class LinkManager implements Function<LinkManager.UrlMeta, Completable> {

  private final CustomTabActivityHelper customTab;
  @SmartLinking private final Preference<Boolean> globalSmartLinkingPref;

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of per-service prefs
  private final ArrayMap<String, Boolean> dumbCache = new ArrayMap<>();

  public LinkManager(CustomTabActivityHelper customTab,
      @SmartLinking Preference<Boolean> globalSmartLinkingPref) {
    this.customTab = customTab;
    this.globalSmartLinkingPref = globalSmartLinkingPref;
  }

  public void connect(MainActivity activity) {
    // Invalidate the cache when a new install/update happens or prefs changed
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
    Observable.merge(toV2Observable(RxBroadcastReceiver.create(activity, filter)),
        toV2Observable(globalSmartLinkingPref.asObservable()))
        .to(new ObservableScoper<>(activity))
        .subscribe(o -> dumbCache.clear());
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

  public Completable openUrl(UrlMeta meta) {
    if (meta.uri == null) {
      Toast.makeText(meta.context, R.string.error_no_url, Toast.LENGTH_SHORT)
          .show();
      return Completable.complete();
    }
    Intent intent = new Intent(Intent.ACTION_VIEW, meta.uri);
    if (!globalSmartLinkingPref.get()) {
      openCustomTab(meta.context, meta.uri, meta.accentColor);
      return Completable.complete();
    }

    if (!dumbCache.containsKey(meta.uri.getHost())) {
      return queryAndOpen(meta.context, meta.uri, intent, meta.accentColor);
    } else if (dumbCache.get(meta.uri.getHost())) {
      meta.context.startActivity(intent);
      return Completable.complete();
    } else {
      openCustomTab(meta.context, meta.uri, meta.accentColor);
      return Completable.complete();
    }
  }

  private Completable queryAndOpen(Context context,
      Uri uri,
      Intent intent,
      @ColorInt int accentColor) {
    PackageManager manager = context.getPackageManager();
    return Observable.defer(() -> Observable.fromIterable(manager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY)))
        .filter(resolveInfo -> isSpecificUriMatch(resolveInfo.match))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .compose(doOnEmpty(() -> {
          dumbCache.put(uri.getHost(), false);
          openCustomTab(context, uri, accentColor);
        }))
        .doOnNext(o -> {
          dumbCache.put(uri.getHost(), true);
          context.startActivity(intent);
        })
        .ignoreElements();
  }

  private void openCustomTab(Context context, Uri uri, @ColorInt int accentColor) {
    customTab.openCustomTab(context,
        customTab.getCustomTabIntent()
            .setStartAnimations(context, R.anim.slide_up, R.anim.inset)
            .setExitAnimations(context, R.anim.outset, R.anim.slide_down)
            .setToolbarColor(accentColor)
            .build(),
        uri);
  }

  @Override public Completable apply(UrlMeta meta) throws Exception {
    return openUrl(meta);
  }

  public static class UrlMeta {
    final Uri uri;
    @ColorInt final int accentColor;
    final Context context;

    public UrlMeta(@Nullable String url, @ColorInt int accentColor, Context context) {
      this(TextUtils.isEmpty(url) ? null : Uri.parse(url), accentColor, context);
    }

    public UrlMeta(@Nullable Uri uri, @ColorInt int accentColor, Context context) {
      this.uri = uri;
      this.accentColor = accentColor;
      this.context = context;
    }
  }
}
