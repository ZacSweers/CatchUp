package io.sweers.catchup.app

import com.bugsnag.android.Bugsnag
import com.squareup.leakcanary.RefWatcher
import io.sweers.catchup.BuildConfig
import timber.log.Timber

class ReleaseCatchUpApplication : CatchUpApplication() {
  override fun initVariant() {
    CatchUpApplication.refWatcher = RefWatcher.DISABLED
    Bugsnag.init(this, BuildConfig.BUGSNAG_KEY)

    val tree = BugsnagTree()
    Bugsnag.getClient()
        .beforeNotify { error ->
          tree.update(error)
          true
        }

    Timber.plant(tree)
  }
}
