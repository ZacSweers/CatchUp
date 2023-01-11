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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.compose.DraggableItem
import dev.zacsweers.catchup.compose.dragContainer
import dev.zacsweers.catchup.compose.rememberDragDropState
import dev.zacsweers.catchup.di.AppScope
import dev.zacsweers.catchup.di.android.FragmentKey
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.InjectableBaseFragment
import io.sweers.catchup.databinding.FragmentOrderServicesBinding
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.util.setLightStatusBar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/*
 * This is a WIP implementation of OrderServices view with Compose.
 *
 * TODO:
 *  * Syllabus handling - requires integrating tap target on the location
 */

private class OrderServicesViewModel(
  serviceMetasMap: Map<String, ServiceMeta>,
  private val catchUpPreferences: CatchUpPreferences
) : ViewModel() {

  private val storedOrder: StateFlow<List<String>>
  private val _serviceMetas: MutableStateFlow<List<ServiceMeta>>
  val serviceMetas: StateFlow<List<ServiceMeta>>
    get() = _serviceMetas
  val canSave: StateFlow<Boolean>

  init {
    // TODO this is bad, switch to collectAsState in the future
    val initialOrder =
      runBlocking { catchUpPreferences.servicesOrder.first() }?.split(",") ?: emptyList()
    val initialOrderedServices = serviceMetasMap.values.sortedBy { initialOrder.indexOf(it.id) }
    storedOrder =
      catchUpPreferences.servicesOrder
        .drop(1) // Ignore the initial value we read synchronously
        .map { it?.split(",") ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, initialOrder)
    _serviceMetas = MutableStateFlow(initialOrderedServices)

    viewModelScope.launch {
      storedOrder
        .map { newOrder -> serviceMetasMap.values.sortedBy { newOrder.indexOf(it.id) } }
        .collect { _serviceMetas.value = it }
    }

    canSave =
      serviceMetas
        .map { it != initialOrderedServices }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
  }

  fun shuffle() {
    val items = serviceMetas.value.toMutableList()
    items.shuffle()
    _serviceMetas.value = items
  }

  fun updateOrder(fromIndex: Int, toIndex: Int) {
    _serviceMetas.value =
      serviceMetas.value.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
  }

  suspend fun save() {
    catchUpPreferences.edit {
      it[CatchUpPreferences.Keys.servicesOrder] =
        serviceMetas.value.joinToString(",", transform = ServiceMeta::id)
    }
  }

  fun shouldShowSyllabus(): Boolean {
    // TODO implement this
    return false
  }
}

@FragmentKey(OrderServicesFragment2::class)
@ContributesMultibinding(AppScope::class, boundType = Fragment::class)
class OrderServicesFragment2
@Inject
constructor(
  private val serviceMetas: Map<String, ServiceMeta>,
  private val catchUpPreferences: CatchUpPreferences,
  private val appConfig: AppConfig,
) : InjectableBaseFragment<FragmentOrderServicesBinding>() {

  // Have to do this here because we can't set a factory separately
  @Suppress("UNCHECKED_CAST")
  private val viewModel by
    viewModels<OrderServicesViewModel> {
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return OrderServicesViewModel(serviceMetas, catchUpPreferences) as T
        }
      }
    }

  // TODO remove with viewbinding API change
  override val bindingInflater:
    (LayoutInflater, ViewGroup?, Boolean) -> FragmentOrderServicesBinding =
    FragmentOrderServicesBinding::inflate

  override fun initView(inflater: LayoutInflater, container: ViewGroup?): View {
    return ComposeView(inflater.context).apply {
      setLightStatusBar(appConfig)
      setContent {
        CatchUpTheme {
          // Have to pass the viewmodel here rather than use compose' viewModel() because otherwise
          // it
          // will try the reflective instantiation since the factory isn't initialized because lol
          ScaffoldContent(viewModel)
        }
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun ScaffoldContent(viewModel: OrderServicesViewModel) {
    val canSave: Boolean by viewModel.canSave.collectAsState()
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(stringResource(id = R.string.pref_reorder_services)) },
          navigationIcon = {
            IconButton(
              onClick = ::onBackPressed,
              content = {
                Image(
                  painterResource(id = R.drawable.ic_arrow_back_black_24dp),
                  stringResource(id = R.string.back)
                )
              }
            )
          },
          actions = {
            IconButton(
              onClick = viewModel::shuffle,
              content = {
                Image(
                  painterResource(R.drawable.ic_shuffle_black_24dp),
                  stringResource(R.string.shuffle)
                )
              }
            )
          }
        )
      },
      content = { paddingValues -> ListContent(paddingValues, viewModel) },
      floatingActionButton = {
        AnimatedVisibility(
          visible = canSave,
          enter = expandIn(expandFrom = Alignment.Center),
          exit = shrinkOut(shrinkTowards = Alignment.Center)
        ) {
          // TODO ripple color?
          val scope = rememberCoroutineScope()
          FloatingActionButton(
            modifier =
              Modifier.indication(
                  MutableInteractionSource(),
                  indication = rememberRipple(color = Color.White)
                )
                .onGloballyPositioned { coordinates ->
                  val (x, y) = coordinates.positionInRoot()
                  // TODO show syllabus on fab
                },
            containerColor = colorResource(R.color.colorAccent),
            onClick = {
              scope.launch {
                viewModel.save()
                activity?.finish()
              }
            },
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

  @Composable
  private fun ListContent(paddingValues: PaddingValues, viewModel: OrderServicesViewModel) {
    val items by viewModel.serviceMetas.collectAsState()
    Surface(Modifier.fillMaxSize().padding(paddingValues)) {
      val listState = rememberLazyListState()
      val dragDropState = rememberDragDropState(listState, viewModel::updateOrder)

      LazyColumn(
        modifier = Modifier.dragContainer(dragDropState),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
          // TODO show touch response on press
          DraggableItem(dragDropState, index) { isDragging ->
            val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp)
            // TODO using m2 card because wtf material3 why can't you just be normal
            Card(elevation = elevation) { ServiceListItem(item) }
          }
        }
      }
    }
  }

  override fun onBackPressed(): Boolean {
    if (viewModel.canSave.value) {
      // TODO circuit-ify this
      AlertDialog.Builder(requireContext())
        .setTitle(R.string.pending_changes_title)
        .setMessage(R.string.pending_changes_message)
        .setNeutralButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .setPositiveButton(R.string.dontsave) { dialog, _ ->
          dialog.dismiss()
          activity?.finish()
        }
        .setNegativeButton(R.string.save) { dialog, _ ->
          dialog.dismiss()
          runBlocking { viewModel.save() }
          activity?.finish()
        }
        .show()
      return true
    }
    activity?.finish()
    return false
  }
}

@Composable
private fun ServiceListItem(item: ServiceMeta) {
  Box(Modifier.background(colorResource(id = item.themeColor)).padding(16.dp)) {
    ConstraintLayout(
      modifier =
        Modifier.fillMaxWidth() // TODO remove this...?
          .wrapContentHeight()
          .wrapContentWidth(Alignment.Start)
    ) {
      val (icon, spacer, text) = createRefs()
      Image(
        painterResource(id = item.icon),
        stringResource(R.string.service_icon),
        modifier =
          Modifier.width(40.dp).height(40.dp).constrainAs(icon) {
            bottom.linkTo(parent.bottom)
            end.linkTo(spacer.start)
            start.linkTo(parent.start)
            top.linkTo(parent.top)
          }
      )
      Spacer(
        modifier =
          Modifier.width(8.dp).height(40.dp).constrainAs(spacer) {
            bottom.linkTo(parent.bottom)
            end.linkTo(text.start)
            start.linkTo(icon.end)
            top.linkTo(parent.top)
          }
      )
      val startRef: ConstrainedLayoutReference = spacer
      Text(
        text = stringResource(id = item.name),
        modifier =
          Modifier.constrainAs(text) {
            bottom.linkTo(parent.bottom)
            end.linkTo(parent.end)
            start.linkTo(startRef.end)
            top.linkTo(parent.top)
          },
        fontStyle = FontStyle.Normal,
        // TODO what about Subhead?
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White
      )
    }
  }
}

@ContributesTo(AppScope::class)
@Module
abstract class OrderServicesModule2 {

  @Multibinds abstract fun serviceMetas(): Map<String, ServiceMeta>
}
