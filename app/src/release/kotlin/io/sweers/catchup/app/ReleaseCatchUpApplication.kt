package io.sweers.catchup.app

import com.bugsnag.android.Bugsnag
import com.squareup.leakcanary.RefWatcher
import io.sweers.catchup.BuildConfig
import timber.log.Timber

class ReleaseCatchUpApplication : CatchUpApplication() {

  override fun inject() {
    DaggerApplicationComponent.builder()
        .application(this)
        .build()
        .inject(this)
  }

  override fun initVariant() {
    CatchUpApplication.refWatcher = RefWatcher.DISABLED
    Bugsnag.init(this, BuildConfig.BUGSNAG_KEY)

    BugsnagTree().also {
      Bugsnag.getClient()
          .beforeNotify { error ->
            it.update(error)
            true
          }

      Timber.plant(it)
    }
  }
}
