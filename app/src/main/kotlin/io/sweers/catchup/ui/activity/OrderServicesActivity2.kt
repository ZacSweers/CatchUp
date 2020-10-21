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
package io.sweers.catchup.ui.activity

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.ConstraintLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorAsset
import androidx.compose.ui.graphics.vector.VectorAssetBuilder
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.loadVectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.accessibilityLabel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.fontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.chibatching.kotpref.bulk
import com.getkeepsafe.taptargetview.TapTarget
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.FragmentComponent
import dagger.multibindings.Multibinds
import dev.zacsweers.catchup.appconfig.AppConfig
import io.sweers.catchup.CatchUpPreferences
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.ColorUtils
import io.sweers.catchup.base.ui.InjectableBaseFragment
import io.sweers.catchup.base.ui.InjectingBaseActivity
import io.sweers.catchup.databinding.FragmentOrderServicesBinding
import io.sweers.catchup.databinding.OrderServicesItemBinding
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.edu.TargetRequest
import io.sweers.catchup.edu.id
import io.sweers.catchup.injection.DaggerMap
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.ui.FontHelper
import io.sweers.catchup.util.asDayContext
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.resolveAttributeColor
import io.sweers.catchup.util.setLightStatusBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

private class OrderServicesViewModel : ViewModel() {

  private val _pendingChanges = MutableStateFlow(emptyList<ServiceMeta>())
  val pendingChanges: StateFlow<List<ServiceMeta>> = _pendingChanges

  private val _canSave = MutableStateFlow(false)
  val canSave: StateFlow<Boolean> = _canSave

  init {
//    storedOrder = catchUpPreferences.servicesOrder?.split(",") ?: emptyList()
    // TODO should we wire this with a flow of stored pref changes?
    viewModelScope.launch {
      pendingChanges
        .drop(1) // skip first
        .distinctUntilChanged()
        .collect { _canSave.value }
    }
  }

  fun updateOrder(items: List<ServiceMeta>) {
    _pendingChanges.value = items
  }

  fun shouldShowSyllabus(): Boolean {
    TODO()
  }

  fun save() {
    check(canSave.value)
    TODO()
    _canSave.value = false
  }
}

@AndroidEntryPoint
class OrderServicesFragment2 : InjectableBaseFragment<FragmentOrderServicesBinding>() {

  @Inject
  lateinit var serviceMetas: DaggerMap<String, ServiceMeta>

  @Inject
  lateinit var catchUpPreferences: CatchUpPreferences

  @Inject
  internal lateinit var syllabus: Syllabus

  @Inject
  internal lateinit var fontHelper: FontHelper

  @Inject
  internal lateinit var appConfig: AppConfig

  // TODO move to viewmodel
  //  - pendingChanges updates
  //  - hasPendingChanges flowAsState
  private lateinit var storedOrder: List<String>
  private var pendingChanges: List<ServiceMeta>? = null
    set(value) {
      var toStore = value
      if (value?.map(ServiceMeta::id)?.sortedBy(storedOrder::indexOf) == storedOrder) {
        toStore = null
      }
      field = toStore
      if (toStore == null) {
        save.hide()
      } else {
        save.show()
      }
    }

  // TODO remove with viewbinding API change
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentOrderServicesBinding =
    FragmentOrderServicesBinding::inflate

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putStringArrayList(
      "pendingChanges",
      pendingChanges?.mapTo(ArrayList(), ServiceMeta::id)
    )
  }

  override fun initView(inflater: LayoutInflater, container: ViewGroup?): View {
    val view = ComposeView(inflater.context)
    view.setContent {
      // TODO control fontFamily globally here
      MaterialTheme {
        prepareContent()
      }
    }
    return view
  }

  @Composable
  private fun prepareContent() {
    val viewModel: OrderServicesViewModel = viewModel()
    // Scaffold with toolbar
    // List

    val canSave by viewModel.canSave.collectAsState()

    Scaffold(
      topBar = {}, // TODO toolbar setup
      bodyContent = { prepareBody(viewModel) }
    )
    // Pending changes
  }

  @Composable
  private fun prepareBody(viewModel: OrderServicesViewModel) {
//    val instanceChanges = savedInstanceState?.getStringArrayList("pendingChanges")
    storedOrder = catchUpPreferences.servicesOrder?.split(",") ?: emptyList()
//    pendingChanges = instanceChanges?.map { serviceMetas[it] as ServiceMeta }
//    val displayOrder = instanceChanges ?: storedOrder
    val displayOrder = storedOrder

    val currentItemsSorted = serviceMetas.values.sortedBy { displayOrder.indexOf(it.id) }
    Surface(Modifier.fillMaxSize()) {
      LazyColumnFor(
        items = currentItemsSorted,
        modifier = Modifier,
        itemContent = { item ->
          Box(
            backgroundColor = colorResource(id = item.themeColor),
            padding = 16.dp
          ) {
            ConstraintLayout(
              modifier = Modifier.fillMaxWidth()
                .wrapContentHeight()
            ) {
              val (icon, text) = createRefs()
              val image = loadVectorResource(id = item.icon)
              image.resource.resource?.let {
                Image(
                  asset = it,
                  modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .accessibilityLabel(R.string.service_icon)
                    .constrainAs(icon) {
                      bottom.linkTo(parent.bottom)
                      end.linkTo(text.start)
                      start.linkTo(parent.start)
                      top.linkTo(parent.top)
                    }
                )
              }
              Text(
                text = stringResource(id = item.name),
                modifier = Modifier.constrainAs(text) {
                  bottom.linkTo(parent.bottom)
                  end.linkTo(parent.end)
                  start.linkTo(icon.end)
                  top.linkTo(parent.top)
                },
                fontStyle = FontStyle.Normal,
                // TODO define this somewhere higher level
                fontFamily = fontFamily(fonts = listOf(ResourceFont(R.font.nunito))),
                // TODO what about Subhead?
                style = MaterialTheme.typography.subtitle1,
                color = Color.White
              )
            }
          }
        }
      )
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    with(activity as AppCompatActivity) {
      if (!isInNightMode()) {
        toolbar.setLightStatusBar(appConfig)
      }
    }
    val lm = LinearLayoutManager(view.context)
    recyclerView.layoutManager = lm
    val instanceChanges = savedInstanceState?.getStringArrayList("pendingChanges")
    storedOrder = catchUpPreferences.servicesOrder?.split(",") ?: emptyList()
    pendingChanges = instanceChanges?.map { serviceMetas[it] as ServiceMeta }
    val displayOrder = instanceChanges ?: storedOrder

    val currentItemsSorted = serviceMetas.values.sortedBy { displayOrder.indexOf(it.id) }
    val adapter = Adapter2(
      // Use a day context since this is like the tablayout UI
      recyclerView.context.asDayContext(),
      currentItemsSorted
    ) { newItemOrder ->
      pendingChanges = if (newItemOrder != currentItemsSorted) {
        newItemOrder
      } else {
        null
      }
    }
    recyclerView.adapter = adapter
    savedInstanceState?.getParcelable<Parcelable>("orderServicesState")?.let(
      lm::onRestoreInstanceState
    )
    toolbar.apply {
      setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
      setNavigationOnClickListener {
        onBackPressed()
      }
      title = context.getString(R.string.pref_reorder_services)
      inflateMenu(R.menu.order_services)
      menu.findItem(R.id.shuffle).setOnMenuItemClickListener {
        adapter.shuffle()
        true
      }
    }
    val callback = MoveCallback2 { start, end -> adapter.move(start, end) }
    ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    save.setOnClickListener {
      pendingChanges?.let { changes ->
        catchUpPreferences.bulk {
          servicesOrder = changes.joinToString(",", transform = ServiceMeta::id)
        }
      }
      activity?.finish()
    }

    val primaryColor = ContextCompat.getColor(save.context, R.color.colorPrimary)
    val textColor = save.context.resolveAttributeColor(android.R.attr.textColorPrimary)
    syllabus.showIfNeverSeen(
      catchUpPreferences::servicesOrderSeen.name,
      TargetRequest(
        target = {
          FabShowTapTarget2(
            delegateTarget = { TapTarget.forView(save, "", "") },
            fab = save,
            title = save.resources.getString(R.string.pref_reorder_services),
            description = save.resources.getString(
              R.string.pref_order_services_description
            )
          )
            .outerCircleColorInt(primaryColor)
            .outerCircleAlpha(0.96f)
            .titleTextColorInt(textColor)
            .descriptionTextColorInt(
              ColorUtils.modifyAlpha(textColor, 0.2f)
            )
            .targetCircleColorInt(textColor)
            .transparentTarget(true)
            .drawShadow(true)
            .id("Save")
            .apply { fontHelper.getFont()?.let(::textTypeface) }
        },
        postDisplay = save::hide
      )
    )
  }

  override fun onBackPressed(): Boolean {
    if (pendingChanges != null) {
      AlertDialog.Builder(save.context)
        .setTitle(R.string.pending_changes_title)
        .setMessage(R.string.pending_changes_message)
        .setNeutralButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .setPositiveButton(R.string.dontsave) { dialog, _ ->
          dialog.dismiss()
          activity?.finish()
        }
        .setNegativeButton(R.string.save) { dialog, _ ->
          dialog.dismiss()
          save.performClick()
        }
        .show()
      return true
    }
    activity?.finish()
    return false
  }
}

// TODO define this somewhere higher level
@Composable
fun Modifier.accessibilityLabel(@StringRes resId: Int) = composed {
  val res = stringResource(resId)
  semantics {
    accessibilityLabel = res
  }
}

private class Adapter2(
  private val context: Context,
  inputItems: List<ServiceMeta>,
  private val changeListener: (List<ServiceMeta>) -> Unit
) : RecyclerView.Adapter<Holder2>() {

  private val items = inputItems.toMutableList()

  init {
    setHasStableIds(true)
  }

  fun shuffle() {
    val current = items.toList()
    items.shuffle()
    DiffUtil.calculateDiff(
      object : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          return current[oldItemPosition].id == items[newItemPosition].id
        }

        override fun getOldListSize() = current.size

        override fun getNewListSize() = items.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          return current[oldItemPosition] == items[newItemPosition]
        }
      }
    ).dispatchUpdatesTo(this)
    changeListener(items)
  }

  override fun getItemId(position: Int): Long {
    return items[position].id.hashCode().toLong()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder2 {
    val itemView = LayoutInflater.from(context).inflate(
      R.layout.order_services_item,
      parent,
      false
    )
    return Holder2(itemView)
  }

  override fun getItemCount() = items.size

  override fun onBindViewHolder(holder: Holder2, position: Int) {
    holder.bind(items[position])
  }

  fun move(start: Int, end: Int) {
    if (start < end) {
      for (i in start until end) {
        Collections.swap(items, i, i + 1)
      }
    } else {
      for (i in start downTo end + 1) {
        Collections.swap(items, i, i - 1)
      }
    }
    changeListener(items.toList())
    notifyItemMoved(start, end)
  }
}

private class Holder2(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val binding = OrderServicesItemBinding.bind(itemView)
  private val container = binding.container
  private val title = binding.title
  private val icon = binding.icon
  private val raise = itemView.resources.getDimensionPixelSize(R.dimen.touch_raise).toFloat()
  private val elevationAnimator = AnimatorInflater.loadStateListAnimator(
    itemView.context,
    R.animator.raise
  )

  fun bind(meta: ServiceMeta) {
    title.setText(meta.name)
    container.setBackgroundColor(
      ContextCompat.getColor(title.context, meta.themeColor)
    )
    icon.setImageResource(meta.icon)
  }

  fun updateSelection(selected: Boolean) {
    container.elevation = if (selected) raise else 0F
    if (selected) {
      container.stateListAnimator = null
    } else {
      container.stateListAnimator = elevationAnimator
    }
  }
}

private class MoveCallback2(
  private val callback: (Int, Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
  ItemTouchHelper.UP or ItemTouchHelper.DOWN,
  0
) {
  override fun onMove(
    recyclerView: RecyclerView,
    viewHolder: ViewHolder,
    target: ViewHolder
  ): Boolean {
    callback(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    return true
  }

  override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
    // Noop
  }

  override fun isLongPressDragEnabled() = true

  override fun isItemViewSwipeEnabled() = false

  override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
    super.onSelectedChanged(viewHolder, actionState)
    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
      viewHolder?.let {
        (it as Holder2).updateSelection(true)
      }
    }
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
    super.clearView(recyclerView, viewHolder)
    (viewHolder as Holder2).updateSelection(false)
  }
}

@InstallIn(FragmentComponent::class)
@Module
abstract class OrderServicesModule2 {

  @Multibinds
  abstract fun serviceMetas(): Map<String, ServiceMeta>
}

private class FabShowTapTarget2(
  private val delegateTarget: () -> TapTarget,
  private val fab: FloatingActionButton,
  title: CharSequence,
  description: CharSequence?
) : TapTarget(title, description) {

  override fun bounds(): Rect {
    val location = IntArray(2)
    fab.getLocationOnScreen(location)
    return Rect(
      location[0],
      location[1],
      location[0] + fab.width,
      location[1] + fab.height
    )
  }

  override fun onReady(runnable: Runnable) {
    fab.doOnLayout {
      if (fab.isShown) {
        delegateTarget().onReady(runnable)
      } else {
        fab.show(
          object : OnVisibilityChangedListener() {
            override fun onShown(fab: FloatingActionButton) {
              delegateTarget().onReady(runnable)
            }
          }
        )
      }
    }
  }
}
