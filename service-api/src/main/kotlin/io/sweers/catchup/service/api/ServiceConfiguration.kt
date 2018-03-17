package io.sweers.catchup.service.api

import android.app.Activity
import android.support.annotation.XmlRes

sealed class ServiceConfiguration {
  class ActivityConfiguration(val activity: Class<out Activity>) : ServiceConfiguration()
  class PreferencesConfiguration(@XmlRes val preferenceResource: Int) : ServiceConfiguration()
}
