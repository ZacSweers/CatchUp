package io.sweers.catchup.edu

import android.app.Activity
import android.content.SharedPreferences
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetSequence.Listener
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.uber.autodispose.kotlin.autoDisposeWith
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.ui.base.BaseActivity
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * Syllabus is a helper that orchestrates feature EDUs and hints via TapTargets.
 */
@PerActivity
class Syllabus @Inject constructor(
    private val activity: Activity,
    private val preferences: SharedPreferences) {

  private val queue = PublishRelay.create<TargetRequest>()
  private var displaying = BehaviorRelay.createDefault(false)

  fun bind(activity: BaseActivity) {
    // TODO would be nice to handle starting mid-sequence for state restoration someday
    // Debounced buffer
    queue.buffer(queue.debounce(2, SECONDS))
        .delay { displaying.filter { !it } }
        .observeOn(mainThread())
        .autoDisposeWith(activity)
        .subscribe { requests ->
          show(requests)
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
    queue.accept(request)
  }

  private fun show(requests: List<TargetRequest>) {
    displaying.accept(true)
    var index = 0
    requests[index].preDisplay?.invoke()
    TapTargetSequence(activity)
        .targets(requests.map { it.target() })
        .considerOuterCircleCanceled(false)
        .continueOnCancel(true)
        .listener(object : Listener {
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
        })
        .start()
  }

}

@Suppress("NOTHING_TO_INLINE")
inline fun TapTarget.id(id: String): TapTarget = id(id.hashCode())

/**
 * @property preDisplay a hook for pre-display callbacks. Note that you may want to create a custom
 * TapTarget instead and override [TapTarget.onReady] for a more dynamic waiting.
 */
data class TargetRequest(
    val target: () -> TapTarget,
    val preDisplay: (() -> Unit)? = null,
    val postDisplay: (() -> Unit)? = null
)
