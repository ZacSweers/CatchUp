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
package io.sweers.catchup.ui.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.jakewharton.rxrelay2.BehaviorRelay
import com.uber.autodispose.lifecycle.CorrespondingEventsFunction
import com.uber.autodispose.lifecycle.LifecycleScopeProvider
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.ui.ViewContainer
import io.sweers.catchup.ui.base.ActivityEvent.CREATE
import io.sweers.catchup.ui.base.ActivityEvent.DESTROY
import io.sweers.catchup.ui.base.ActivityEvent.PAUSE
import io.sweers.catchup.ui.base.ActivityEvent.RESUME
import io.sweers.catchup.ui.base.ActivityEvent.START
import io.sweers.catchup.ui.base.ActivityEvent.STOP
import io.sweers.catchup.util.updateNavBarColor
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), LifecycleScopeProvider<ActivityEvent> {

  private val lifecycleRelay = BehaviorRelay.create<ActivityEvent>()

  protected inline fun <T, R> Observable<T>.doOnCreate(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == CREATE) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnStart(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == START) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnResume(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == RESUME) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnPause(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == PAUSE) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnStop(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == STOP) {
        r.action()
      }
    }
  }

  protected inline fun <T, R> Observable<T>.doOnDestroy(
    r: R,
    crossinline action: R.() -> Unit
  ): Observable<T> = apply {
    doOnNext {
      if (it == DESTROY) {
        r.action()
      }
    }
  }

  @SuppressLint("AutoDispose")
  protected inline fun <T> T.doOnDestroy(crossinline action: T.() -> Unit): T = apply {
    lifecycle().doOnDestroy(this) { action() }.subscribe()
  }

  @Inject
  protected lateinit var viewContainer: ViewContainer
  @Inject
  protected lateinit var catchUpPreferences: CatchUpPreferences

  @CheckResult
  override fun lifecycle(): Observable<ActivityEvent> {
    return lifecycleRelay
  }

  final override fun correspondingEvents(): CorrespondingEventsFunction<ActivityEvent> {
    return ActivityEvent.LIFECYCLE
  }

  final override fun peekLifecycle(): ActivityEvent? {
    return lifecycleRelay.value
  }

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    lifecycleRelay.accept(CREATE)
  }

  @CallSuper
  override fun onStart() {
    super.onStart()
    lifecycleRelay.accept(START)
  }

  @CallSuper
  override fun onResume() {
    super.onResume()
    lifecycleRelay.accept(RESUME)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this)
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    updateNavBarColor(catchUpPreferences = catchUpPreferences)
  }

  @CallSuper
  override fun onPause() {
    lifecycleRelay.accept(PAUSE)
    super.onPause()
  }

  @CallSuper
  override fun onStop() {
    lifecycleRelay.accept(STOP)
    super.onStop()
  }

  @CallSuper
  override fun onDestroy() {
    lifecycleRelay.accept(DESTROY)
    super.onDestroy()
  }

  override fun onBackPressed() {
    supportFragmentManager.fragments.filterIsInstance<BackpressHandler>().forEach {
      if (it.onBackPressed()) {
        return
      }
    }
    super.onBackPressed()
  }
}
