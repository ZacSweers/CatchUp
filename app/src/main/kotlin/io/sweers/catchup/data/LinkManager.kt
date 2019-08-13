/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.data

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.collection.ArrayMap
import androidx.core.util.toAndroidPair
import androidx.lifecycle.lifecycleScope
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.flowFor
import io.sweers.catchup.flowbinding.intentReceivers
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.service.api.ImageViewerData
import io.sweers.catchup.service.api.LinkHandler
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.activity.ImageViewerActivity
import io.sweers.catchup.ui.activity.MainActivity
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.kotlin.any
import io.sweers.catchup.util.kotlin.applyIf
import io.sweers.catchup.util.kotlin.mergeWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@PerActivity
class LinkManager @Inject constructor(
  private val customTab: CustomTabActivityHelper,
  private val activity: Activity,
  private val catchUpPreferences: CatchUpPreferences
) : LinkHandler {

  // Naive cache that tracks if we've already resolved for activities that can handle a given host
  // TODO Eventually replace this with something that's mindful of per-service prefs
  private val dumbCache = ArrayMap<String, Boolean>()

  fun connect(activity: MainActivity) {
    // Invalidate the cache when a new install/update happens or prefs changed
    val filter = IntentFilter()
    if (Build.VERSION.SDK_INT < 29) {
      @Suppress("DEPRECATION")
      filter.addAction(Intent.ACTION_INSTALL_PACKAGE)
    }
    filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
    activity.lifecycleScope.launch {
      activity.intentReceivers(filter)
          .mergeWith(catchUpPreferences.flowFor { ::smartlinkingGlobal })
          .collect { dumbCache.clear() }
    }
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
  override suspend fun openUrl(meta: UrlMeta) {
    meta.imageViewerData?.let { imageData ->
      val intent = Intent(activity, ImageViewerActivity::class.java)
      intent.putExtra(ImageViewerActivity.INTENT_ID, imageData.id)
      intent.putExtra(ImageViewerActivity.INTENT_URL, imageData.imageUrl)
      intent.putExtra(ImageViewerActivity.INTENT_SOURCE_URL, imageData.sourceUrl)
      val options = getActivityOptions(imageData)
      activity.startActivityForResult(intent, 101, options.toBundle())
      return
    }
    if (meta.isSupportedInMediaViewer()) {
      val intent = Intent(activity, ImageViewerActivity::class.java)
      val url = meta.uri.toString()
      intent.putExtra(ImageViewerActivity.INTENT_URL, url)
      intent.putExtra(ImageViewerActivity.INTENT_SOURCE_URL, url)
      activity.startActivityForResult(intent, 102)
      return
    }
    val uri = meta.uri ?: run {
      Toast.makeText(meta.context, R.string.error_no_url, Toast.LENGTH_SHORT)
          .show()
      return
    }
    val intent = Intent(Intent.ACTION_VIEW, meta.uri)
    if (!catchUpPreferences.smartlinkingGlobal) {
      openCustomTab(meta.context, uri, meta.accentColor)
      return
    }

    if (!dumbCache.containsKey(uri.host)) {
      queryAndOpen(meta.context, uri, intent, meta.accentColor)
    } else if (dumbCache[uri.host] == true) {
      meta.context.startActivity(intent)
    } else {
      openCustomTab(meta.context, uri, meta.accentColor)
    }
  }

  private fun getActivityOptions(imageData: ImageViewerData): ActivityOptions {
    val imagePair = (imageData.image to activity.getString(
        R.string.transition_image)).toAndroidPair()
    val decorView = activity.window.decorView
    val statusBackground: View = decorView.findViewById(android.R.id.statusBarBackground)
    val navBackground: View? = decorView.findViewById(android.R.id.navigationBarBackground)
    val statusPair = Pair(statusBackground,
        statusBackground.transitionName).toAndroidPair()

    return if (navBackground == null) {
      ActivityOptions.makeSceneTransitionAnimation(activity,
          imagePair, statusPair)
    } else {
      val navPair = Pair(navBackground, navBackground.transitionName).toAndroidPair()
      ActivityOptions.makeSceneTransitionAnimation(activity,
          imagePair, statusPair, navPair)
    }
  }

  private suspend fun queryAndOpen(
    context: Context,
    uri: Uri,
    intent: Intent,
    @ColorInt accentColor: Int
  ) {
    val manager = context.packageManager
    val matchedUri = flow {
      manager.queryIntentActivities(intent,
          PackageManager.MATCH_DEFAULT_ONLY).forEach {
        emit(it)
      }
    }.flowOn(Dispatchers.IO)
        .any { resolveInfo -> isSpecificUriMatch(resolveInfo.match) }

    if (matchedUri) {
      dumbCache[uri.host] = true
      context.startActivity(intent)
    } else {
      dumbCache[uri.host] = false
      openCustomTab(context, uri, accentColor)
    }
  }

  private fun openCustomTab(context: Context, uri: Uri, @ColorInt accentColor: Int) {
    customTab.openCustomTab(context,
        customTab.customTabIntent
            .applyIf(Build.VERSION.SDK_INT < 29) {
              // I like the Q animations, so don't override these there 😬
              setStartAnimations(context, R.anim.slide_up, R.anim.inset)
              setExitAnimations(context, R.anim.outset, R.anim.slide_down)
            }
            .setToolbarColor(accentColor)
            .build(),
        uri)
  }
}
