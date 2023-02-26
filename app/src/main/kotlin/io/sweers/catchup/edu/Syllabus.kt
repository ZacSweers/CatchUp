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
package io.sweers.catchup.edu

import android.app.Activity
import android.content.SharedPreferences
import androidx.lifecycle.lifecycleScope
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetSequence.Listener
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.SingleIn
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread
import io.sweers.catchup.ui.activity.MainActivity
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** Syllabus is a helper that orchestrates feature EDUs and hints via TapTargets. */
// TODO compose/coroutines-ify this
@SingleIn(AppScope::class)
class Syllabus @Inject constructor(private val preferences: SharedPreferences) {

  private val queue = PublishRelay.create<TargetRequest>()
  private var displaying = BehaviorRelay.createDefault(false)

  fun bind(activity: MainActivity) {
    // TODO coroutines-ify this
    // Debounced buffer
    val disposable =
      queue
        .buffer(queue.debounce(1, SECONDS))
        .delay { displaying.filter { !it } }
        .observeOn(mainThread())
        .subscribe { requests -> show(activity, requests) }
    activity.lifecycleScope.launch {
      suspendCancellableCoroutine { it.invokeOnCancellation { disposable.dispose() } }
    }
  }

  fun showIfNeverSeen(key: String, body: () -> TapTarget) {
    showIfNeverSeen(key, TargetRequest(body))
  }

  fun showIfNeverSeen(key: String, request: TargetRequest) {
    if (!preferences.getBoolean(key, false)) {
      preferences.edit().putBoolean(key, true).apply()
      show(request)
    }
  }

  fun show(body: () -> TapTarget) {
    show(TargetRequest(body))
  }

  fun show(request: TargetRequest) {
    if (!queue.hasObservers()) {
      throw IllegalStateException("Syllabus is not active!")
    }
    queue.accept(request)
  }

  private fun show(activity: Activity, requests: List<TargetRequest>) {
    displaying.accept(true)
    var index = 0
    requests[index].preDisplay?.invoke()
    TapTargetSequence(activity)
      .targets(requests.map { it.target() })
      .considerOuterCircleCanceled(false)
      .continueOnCancel(true)
      .listener(
        object : Listener {
          override fun onSequenceCanceled(lastTarget: TapTarget) {
            displaying.accept(false)
          }

          override fun onSequenceFinish() {
            displaying.accept(false)
          }

          override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
            // TODO Analytics this?
            requests[index++].postDisplay?.invoke()
            if (index < requests.size) {
              requests[index].preDisplay?.invoke()
            }
          }
        }
      )
      .start()
  }
}

@Suppress("NOTHING_TO_INLINE") inline fun TapTarget.id(id: String): TapTarget = id(id.hashCode())

/**
 * @property preDisplay a hook for pre-display callbacks. Note that you may want to create a custom
 *   TapTarget instead and override [TapTarget.onReady] for a more dynamic waiting.
 */
data class TargetRequest(
  val target: () -> TapTarget,
  val preDisplay: (() -> Unit)? = null,
  val postDisplay: (() -> Unit)? = null
)
