package io.sweers.catchup.changes

import `in`.uncod.android.bypass.Bypass
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.support.annotation.ColorInt
import android.support.design.widget.BottomSheetDialog
import android.support.v7.widget.Toolbar
import android.text.style.StyleSpan
import android.widget.TextView
import com.getkeepsafe.taptargetview.TapTarget
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.edu.HintArbiter
import io.sweers.catchup.edu.id
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.FontArbiter
import io.sweers.catchup.util.ColorUtils
import io.sweers.catchup.util.LinkTouchMovementMethod
import io.sweers.catchup.util.TouchableUrlSpan
import io.sweers.catchup.util.ifNotEmpty
import io.sweers.catchup.util.markdown
import io.sweers.catchup.util.parseMarkdownAndPlainLinks
import io.sweers.catchup.util.resolveActivity
import javax.inject.Inject

class ChangelogArbiter @Inject constructor(
    private val linkManager: LinkManager,
    private val bypass: Bypass,
    private val fontArbiter: FontArbiter,
    private val hintArbiter: HintArbiter,
    private val sharedPreferences: SharedPreferences) {

  fun bindWith(toolbar: Toolbar, @ColorInt hintColor: Int, linkColor: () -> Int) {
    val changelog = toolbar.resources.getString(R.string.changelog_text)
    // Check if version name changed and if there's a changelog
    if (sharedPreferences.getString("last_version", null) != BuildConfig.VERSION_NAME) {
      // Write the new version in
      sharedPreferences.edit().putString("last_version", BuildConfig.VERSION_NAME).apply()
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
        hintArbiter.showIfNeverSeen("changelog_seen") {
          TapTarget.forToolbarMenuItem(toolbar, R.id.changes, "Changes",
              "Click here for new changes")
              .outerCircleColorInt(hintColor)
              .outerCircleAlpha(0.96f)
              .targetCircleColor(R.color.colorPrimary)
              .titleTextColorInt(Color.WHITE)
              .descriptionTextColorInt(Color.parseColor("#33FFFFFF"))
              .drawShadow(true)
              .id("changelog")
              .apply {
                // fontArbiter.getFont()?.let(::textTypeface)  // Uncomment this to make the kotlin compiler explode
                fontArbiter.getFont()?.let {
                  textTypeface(it)
                }
              }
        }
      }
    }
  }

  private fun showChangelog(changelog: String,
      context: Context,
      @ColorInt highlightColor: Int): Boolean {
    // TODO Make this a custom controller instead
    // TODO Animate this! Inspiration - https://dribbble.com/shots/3965908--Place-Notification
    //   and https://robinhood.engineering/beautiful-animations-using-android-constraintlayout-eee5b72ecae3
    BottomSheetDialog(context)
        .apply {
          val contentView = layoutInflater
              .inflate(R.layout.controller_whatsnew, null, false)
          setContentView(contentView)
          contentView.findViewById<TextView>(R.id.build_name)?.text = BuildConfig.VERSION_NAME
          contentView.findViewById<TextView>(R.id.changes)?.let { changesTextView ->
            changesTextView.movementMethod = LinkTouchMovementMethod.getInstance()
            changesTextView.highlightColor = highlightColor
            changesTextView.setLinkTextColor(highlightColor)
            changesTextView.text = changelog
                .markdown()
                .parseMarkdownAndPlainLinks(
                    on = changesTextView,
                    with = bypass,
                    alternateSpans = { url: String ->
                      setOf(
                          object : TouchableUrlSpan(url,
                              ColorStateList.valueOf(highlightColor),
                              ColorUtils.modifyAlpha(highlightColor, 0.1f)) {
                            override fun onClick(url: String) {
                              linkManager.openUrl(
                                  UrlMeta(url, highlightColor, context.resolveActivity()))
                                  .subscribe()
                            }
                          },
                          StyleSpan(Typeface.BOLD)
                      )
                    })
          }
        }
        .show()
    return true
  }

}
