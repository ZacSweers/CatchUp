/*
 * Copyright (C) 2020. Zac Sweers
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
package io.sweers.catchup.ui.activity

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slack.circuit.CircuitUiState
import com.slack.circuit.Navigator
import com.slack.circuit.Presenter
import com.slack.circuit.Screen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.zacsweers.catchup.circuit.DialogOverlay
import dev.zacsweers.catchup.circuit.DialogResult
import dev.zacsweers.catchup.compose.DraggableItem
import dev.zacsweers.catchup.compose.dragContainer
import dev.zacsweers.catchup.compose.rememberDragDropState
import dev.zacsweers.catchup.di.AppScope
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BackPressNavButton
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.util.asDayContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
object OrderServicesScreen : Screen {
  data class State(
    val services: SnapshotStateList<ServiceMeta>?,
    val showSave: Boolean = false,
    val showConfirmation: Boolean = false,
    val eventSink: (Event) -> Unit = {},
  ) : CircuitUiState
  sealed interface Event {
    object Shuffle : Event
    data class Reorder(val from: Int, val to: Int) : Event
    object BackPress : Event
    object Save : Event
    data class DismissConfirmation(val save: Boolean, val pop: Boolean) : Event
  }
}

// TODO Syllabus handling - requires integrating tap target on the location
class OrderServicesPresenter
@AssistedInject
constructor(
  @Assisted private val navigator: Navigator,
  private val serviceMetas: Map<String, ServiceMeta>,
  private val catchUpPreferences: CatchUpPreferences,
) : Presenter<OrderServicesScreen.State> {
  @CircuitInject(OrderServicesScreen::class, AppScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): OrderServicesPresenter
  }

  @Composable
  override fun present(): OrderServicesScreen.State {
    val storedOrderState by remember { catchUpPreferences.servicesOrder }.collectAsState(null)

    return storedOrderState?.let { storedOrder ->
      val initialOrderedServices =
        remember(storedOrder) {
          serviceMetas.values.sortedBy { storedOrder.indexOf(it.id) }.toImmutableList()
        }

      val currentDisplay =
        remember(storedOrder) {
          serviceMetas.values.sortedBy { storedOrder.indexOf(it.id) }.toMutableStateList()
        }

      val isChanged by remember {
        derivedStateOf {
          val initial = initialOrderedServices.joinToString { it.id }
          val current = currentDisplay.joinToString { it.id }
          initial != current
        }
      }

      var showConfirmation by remember { mutableStateOf(false) }

      BackHandler(enabled = isChanged && !showConfirmation) { showConfirmation = true }

      val scope = rememberCoroutineScope()
      return OrderServicesScreen.State(
        services = currentDisplay,
        showSave = isChanged,
        showConfirmation = showConfirmation,
      ) { event ->
        when (event) {
          is OrderServicesScreen.Event.Reorder -> {
            currentDisplay.apply { add(event.to, removeAt(event.from)) }
          }
          OrderServicesScreen.Event.Shuffle -> {
            currentDisplay.shuffle()
          }
          OrderServicesScreen.Event.Save -> {
            scope.launch {
              save(currentDisplay)
              navigator.pop()
            }
          }
          is OrderServicesScreen.Event.DismissConfirmation -> {
            showConfirmation = false
            if (event.save) {
              scope.launch {
                save(currentDisplay)
                navigator.pop()
              }
            } else if (event.pop) {
              navigator.pop()
            }
          }
          OrderServicesScreen.Event.BackPress -> {
            if (!showConfirmation && isChanged) {
              showConfirmation = true
            } else {
              navigator.pop()
            }
          }
        }
      }
    }
      ?: OrderServicesScreen.State(services = null)
  }

  private suspend fun save(newOrder: List<ServiceMeta>) {
    catchUpPreferences.edit {
      it[CatchUpPreferences.Keys.servicesOrder] =
        newOrder.joinToString(",", transform = ServiceMeta::id)
    }
  }
}

@CircuitInject(OrderServicesScreen::class, AppScope::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OrderServices(state: OrderServicesScreen.State, modifier: Modifier = Modifier) {
  val eventSink = state.eventSink
  if (state.showConfirmation) {
    val overlayHost = LocalOverlayHost.current
    LaunchedEffect(Unit) {
      val result =
        overlayHost.show(
          DialogOverlay(
            title = { Text(stringResource(R.string.pending_changes_title)) },
            text = { Text(stringResource(R.string.pending_changes_message)) },
            confirmButtonText = { Text(stringResource(R.string.save)) },
            dismissButtonText = { Text(stringResource(R.string.dontsave)) }
          )
        )
      when (result) {
        DialogResult.Cancel -> {
          eventSink(OrderServicesScreen.Event.DismissConfirmation(save = false, pop = true))
        }
        DialogResult.Confirm -> {
          eventSink(OrderServicesScreen.Event.DismissConfirmation(save = true, pop = false))
        }
        DialogResult.Dismiss -> {
          eventSink(OrderServicesScreen.Event.DismissConfirmation(save = false, pop = false))
        }
      }
    }
  }
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(id = R.string.pref_reorder_services)) },
        navigationIcon = { BackPressNavButton() },
        actions = {
          IconButton(
            onClick = { eventSink(OrderServicesScreen.Event.Shuffle) },
            content = {
              Icon(
                painter = painterResource(R.drawable.ic_shuffle_black_24dp),
                contentDescription = stringResource(R.string.shuffle),
              )
            }
          )
        }
      )
    },
    content = { innerPadding ->
      state.services?.let {
        ListContent(it, Modifier.padding(innerPadding)) { from, to ->
          state.eventSink(OrderServicesScreen.Event.Reorder(from, to))
        }
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = state.showSave,
        enter = scaleIn(),
        exit = scaleOut(),
      ) {
        val scope = rememberCoroutineScope()
        FloatingActionButton(
          modifier =
            Modifier.indication(
              MutableInteractionSource(),
              indication = rememberRipple(color = Color.White)
            ),
          // TODO show syllabus on fab
          //              .onGloballyPositioned { coordinates ->
          //                val (x, y) = coordinates.positionInRoot()
          //              },
          containerColor = colorResource(R.color.colorAccent),
          onClick = { scope.launch { eventSink(OrderServicesScreen.Event.Save) } },
          content = {
            Image(
              painterResource(R.drawable.ic_save_black_24dp),
              stringResource(R.string.save),
              colorFilter = ColorFilter.tint(Color.White)
            )
          }
        )
      }
    }
  )
}

@SuppressLint("ComposeMutableParameters") // https://github.com/slackhq/compose-lints/issues/48
@Composable
private fun ListContent(
  services: SnapshotStateList<ServiceMeta>,
  modifier: Modifier = Modifier,
  onMove: (Int, Int) -> Unit,
) {
  Surface(modifier.fillMaxSize()) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState, onMove = onMove)

    LazyColumn(
      modifier = Modifier.dragContainer(dragDropState, LocalHapticFeedback.current),
      state = listState,
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = spacedBy(16.dp)
    ) {
      itemsIndexed(services, key = { _, item -> item.id }) { index, item ->
        // TODO show touch response on press
        DraggableItem(dragDropState, index) { isDragging ->
          val elevation by
            animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "Item Elevation")
          // TODO using m2 card because wtf material3 why can't you just be normal
          Card(elevation = elevation) { ServiceListItem(item) }
        }
      }
    }
  }
}

@Composable
private fun ServiceListItem(item: ServiceMeta) {
  val themeColor = LocalContext.current.asDayContext().getColor(item.themeColor)
  Row(
    modifier = Modifier.fillMaxWidth().background(Color(themeColor)).padding(16.dp),
    horizontalArrangement = spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      painter = painterResource(item.icon),
      contentDescription = stringResource(R.string.service_icon),
      modifier = Modifier.width(40.dp).height(40.dp)
    )

    Text(
      text = stringResource(item.name),
      style = MaterialTheme.typography.headlineSmall,
      color = Color.White
    )
  }
}
