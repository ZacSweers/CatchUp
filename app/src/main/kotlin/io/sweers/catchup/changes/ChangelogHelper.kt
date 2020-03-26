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
package io.sweers.catchup.changes

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.getkeepsafe.taptargetview.TapTarget
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import dev.zacsweers.catchup.appconfig.AppConfig
import io.noties.markwon.Markwon
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.ColorUtils
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.databinding.FragmentWhatsnewBinding
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.edu.id
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.FontHelper
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.TouchableUrlSpan
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.ifNotEmpty
import io.sweers.catchup.util.markdown
import io.sweers.catchup.util.parseMarkdownAndPlainLinks
import io.sweers.catchup.util.resolveActivity
import io.sweers.catchup.util.show
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChangelogHelper @Inject constructor(
  private val linkManager: LinkManager,
  private val markwon: Lazy<Markwon>,
  private val fontHelper: FontHelper,
  private val syllabus: Syllabus,
  private val sharedPreferences: SharedPreferences,
  private val appConfig: AppConfig
) {

  fun bindWith(toolbar: Toolbar, @ColorInt hintColor: Int, linkColor: () -> Int) {
    val changelog = toolbar.resources.getString(R.string.changelog_text)
    val lastVersion = sharedPreferences.getString("last_version", null)
    // Check if version name changed and if there's a changelog
    if (lastVersion != appConfig.versionName) {
      // Write the new version in
      sharedPreferences.edit().putString("last_version", appConfig.versionName).apply()
      if (lastVersion == null) {
        // This was the first load it seems, so ignore it
        return
      }
      changelog.ifNotEmpty {
        toolbar.inflateMenu(R.menu.changes)
        with(toolbar.menu) {
          findItem(R.id.changes).let { item ->
            item.setOnMenuItemClickListener {
              removeItem(R.id.changes)
              showChangelog(changelog, toolbar.context, linkColor())
            }
          }
        }
        syllabus.showIfNeverSeen("changelog_seen") {
          TapTarget.forToolbarMenuItem(toolbar, R.id.changes, "Changes",
              "Click here for new changes")
              .outerCircleColorInt(hintColor)
              .outerCircleAlpha(0.96f)
              .targetCircleColor(R.color.colorPrimary)
              .titleTextColorInt(Color.WHITE)
              .descriptionTextColorInt(Color.parseColor("#33FFFFFF"))
              .drawShadow(true)
              .id("changelog")
              .apply { fontHelper.getFont()?.let(::textTypeface) }
        }
      }
    }
  }

  @SuppressLint("InflateParams")
  private fun showChangelog(
    changelog: String,
    context: Context,
    @ColorInt highlightColor: Int
  ): Boolean {
    // TODO Make this a custom fragment instead, which should make the animation less jarring
    BottomSheetDialog(context)
        .apply {
          val content = FragmentWhatsnewBinding.inflate(layoutInflater)
          setContentView(content.root)
          val title = content.buildName.apply {
            typeface = fontHelper.getFont()
            text = appConfig.versionName
          }
          val changes = content.changes.also { changesTextView ->
            changesTextView.typeface = fontHelper.getFont()
            changesTextView.movementMethod = LinkTouchMovementMethod.getInstance()
            changesTextView.highlightColor = highlightColor
            changesTextView.setLinkTextColor(highlightColor)
            changesTextView.text = changelog
                .markdown()
                .parseMarkdownAndPlainLinks(
                    on = changesTextView,
                    with = markwon.get(),
                    alternateSpans = { url: String ->
                      setOf(
                          object : TouchableUrlSpan(url,
                              ColorStateList.valueOf(highlightColor),
                              ColorUtils.modifyAlpha(highlightColor, 0.1f)) {
                            override fun onClick(url: String) {
                              val resolvedActivity = context.resolveActivity()
                              resolvedActivity.lifecycleScope.launch {
                                linkManager.openUrl(
                                    UrlMeta(url, highlightColor, resolvedActivity))
                              }
                            }
                          },
                          StyleSpan(Typeface.BOLD)
                      )
                    })
          }

          content.root.doOnLayout {
            val duration = 400L
            val height = content.root.bottom
            title.translationY = (height - title.y) * 0.25f
            title.alpha = 0f
            title.show()
            changes.translationY = (height - changes.y) * 0.25f
            changes.alpha = 0f
            changes.show()
            val interpolator = UiUtil.fastOutSlowInInterpolator
            ViewCompat.animate(title)
                .alpha(1f)
                .withLayer()
                .translationY(0f)
                .setInterpolator(interpolator)
                .setDuration(duration)
                .setStartDelay(50)
                .start()
            ViewCompat.animate(changes)
                .alpha(1f)
                .withLayer()
                .translationY(0f)
                .setInterpolator(interpolator)
                .setStartDelay(100)
                .setDuration(duration)
                .start()
          }
        }
        .show()
    return true
  }
}
