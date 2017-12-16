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

package io.sweers.catchup.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.support.annotation.CheckResult
import android.support.annotation.ColorInt
import android.support.v4.util.ArrayMap
import android.widget.Toast
import com.f2prateek.rx.preferences2.Preference
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.rx.doOnEmpty
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.activity.MainActivity
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.registerReceiver
import javax.inject.Inject

@PerActivity
class LinkManager @Inject constructor(private val customTab: CustomTabActivityHelper)
  : LinkHandler {

  private val globalSmartLinkingPref: Preference<Boolean> = P.SmartlinkingGlobal.rx()

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of per-service prefs
  private val dumbCache = ArrayMap<String, Boolean>()

  fun connect(activity: MainActivity) {
    // Invalidate the cache when a new install/update happens or prefs changed
    val filter = IntentFilter()
    filter.addAction(Intent.ACTION_INSTALL_PACKAGE)
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
    Observable.merge(activity.registerReceiver(filter), globalSmartLinkingPref.asObservable())
        .autoDisposable(activity)
        .subscribe { dumbCache.clear() }
  }

  /**
   * Neat little helper method to check if a [android.content.pm.ResolveInfo.match] is for a
   * specific match or not. This is useful for checking if the ResolveInfo instance itself
   * is a browser or not.
   *
   * @param inputMatch the match int as provided by the ResolveInfo result
   * @return `true` if it's a specific Uri match, `false` if not.
   */
  private fun isSpecificUriMatch(inputMatch: Int): Boolean {
    var match = inputMatch
    match = match and IntentFilter.MATCH_CATEGORY_MASK
    return match >= IntentFilter.MATCH_CATEGORY_HOST && match <= IntentFilter.MATCH_CATEGORY_PATH
  }

  @Suppress("MemberVisibilityCanPrivate")
  @CheckResult
  fun openUrl(meta: UrlMeta): Completable {
    if (meta.uri == null) {
      Toast.makeText(meta.context, R.string.error_no_url, Toast.LENGTH_SHORT)
          .show()
      return Completable.complete()
    }
    val uri = meta.uri!!
    val intent = Intent(Intent.ACTION_VIEW, meta.uri)
    if (!globalSmartLinkingPref.get()) {
      openCustomTab(meta.context, uri, meta.accentColor)
      return Completable.complete()
    }

    return if (!dumbCache.containsKey(uri.host)) {
      queryAndOpen(meta.context, uri, intent, meta.accentColor)
    } else if (dumbCache[uri.host] == true) {
      meta.context.startActivity(intent)
      Completable.complete()
    } else {
      openCustomTab(meta.context, uri, meta.accentColor)
      Completable.complete()
    }
  }

  private fun queryAndOpen(context: Context,
      uri: Uri,
      intent: Intent,
      @ColorInt accentColor: Int): Completable {
    val manager = context.packageManager
    return Observable
        .defer<ResolveInfo> {
          Observable.fromIterable<ResolveInfo>(manager.queryIntentActivities(intent,
              PackageManager.MATCH_DEFAULT_ONLY))
        }
        .filter { resolveInfo -> isSpecificUriMatch(resolveInfo.match) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnEmpty {
          dumbCache.put(uri.host, false)
          openCustomTab(context, uri, accentColor)
        }
        .doOnNext {
          dumbCache.put(uri.host, true)
          context.startActivity(intent)
        }
        .ignoreElements()
  }

  private fun openCustomTab(context: Context, uri: Uri, @ColorInt accentColor: Int) {
    customTab.openCustomTab(context,
        customTab.customTabIntent
            .setStartAnimations(context, R.anim.slide_up, R.anim.inset)
            .setExitAnimations(context, R.anim.outset, R.anim.slide_down)
            .setToolbarColor(accentColor)
            .build(),
        uri)
  }

  @Throws(Exception::class)
  override fun apply(meta: UrlMeta) = openUrl(meta)

}
