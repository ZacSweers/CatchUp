package catchup.app.service

import com.slack.circuit.foundation.EventListener
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import java.util.concurrent.atomic.AtomicInteger
import timber.log.Timber

class TimberEventListener(private val tree: Timber.Tree) : EventListener {
  class Factory : EventListener.Factory {
    override fun create(screen: Screen, context: CircuitContext): EventListener {
      val tag = "Circuit-${screen.javaClass.simpleName}"
      val tree = Timber.tag(tag)
      tree.d("Creating event listener")
      return TimberEventListener(tree)
    }
  }

  private var stateEmissionCount = AtomicInteger(0)
  private var lastStateHash = 0

  override fun dispose() {
    tree.d("dispose")
  }

  override fun onAfterCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    presenter: Presenter<*>?,
    context: CircuitContext,
  ) {
    tree.d("onAfterCreatePresenter")
  }

  override fun onAfterCreateUi(screen: Screen, ui: Ui<*>?, context: CircuitContext) {
    tree.d("onAfterCreateUi")
  }

  override fun onBeforeCreatePresenter(
    screen: Screen,
    navigator: Navigator,
    context: CircuitContext,
  ) {
    tree.d("onBeforeCreatePresenter")
  }

  override fun onBeforeCreateUi(screen: Screen, context: CircuitContext) {
    tree.d("onBeforeCreateUi")
  }

  override fun onDisposeContent() {
    tree.d("onDisposeContent")
  }

  override fun onDisposePresent() {
    tree.d("onDisposePresent")
  }

  override fun onStartContent() {
    tree.d("onStartContent")
  }

  override fun onStartPresent() {
    tree.d("onStartPresent")
  }

  override fun onState(state: CircuitUiState) {
    tree.d("onState: ${state::class.simpleName} - ${stateEmissionCount.getAndIncrement()}")
    if (state.hashCode() != lastStateHash) {
      lastStateHash = state.hashCode()
      tree.d("onState hash changed: ${state::class.simpleName} - $lastStateHash")
    }
  }

  override fun onUnavailableContent(
    screen: Screen,
    presenter: Presenter<*>?,
    ui: Ui<*>?,
    context: CircuitContext,
  ) {
    tree.d("onUnavailableContent: ${presenter?.javaClass?.simpleName} ${ui?.javaClass?.simpleName}")
  }

  override fun start() {
    tree.d("start")
  }
}
