/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.activity

import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.doOnLayout
import androidx.fragment.app.transaction
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.getkeepsafe.taptargetview.TapTarget
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.Multibinds
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.edu.Syllabus
import io.sweers.catchup.edu.TargetRequest
import io.sweers.catchup.edu.id
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.service.api.ServiceMeta
import io.sweers.catchup.serviceregistry.ResolvedCatchUpServiceMetaRegistry
import io.sweers.catchup.ui.FontHelper
import io.sweers.catchup.ui.base.InjectableBaseFragment
import io.sweers.catchup.ui.base.InjectingBaseActivity
import io.sweers.catchup.util.ColorUtils
import io.sweers.catchup.util.asDayContext
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.resolveAttributeColor
import io.sweers.catchup.util.setLightStatusBar
import kotterknife.bindView
import java.util.Collections
import javax.inject.Inject

class OrderServicesActivity : InjectingBaseActivity() {

  @Inject
  internal lateinit var syllabus: Syllabus

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_generic_container, viewGroup)
    syllabus.bind(this)

    if (savedInstanceState == null) {
      supportFragmentManager.transaction {
        add(R.id.fragment_container, OrderServicesFragment())
      }
    }
  }

  @dagger.Module
  abstract inner class Module {
    @Binds
    @PerActivity
    abstract fun provideActivity(activity: OrderServicesActivity): Activity
  }
}

class OrderServicesFragment : InjectableBaseFragment() {

  @Inject
  lateinit var serviceMetas: Map<String, @JvmSuppressWildcards ServiceMeta>
  @Inject
  lateinit var sharedPrefs: SharedPreferences
  @Inject
  internal lateinit var syllabus: Syllabus
  @Inject
  internal lateinit var fontHelper: FontHelper
  private val save by bindView<FloatingActionButton>(R.id.save)
  private val toolbar by bindView<Toolbar>(R.id.toolbar)
  private val recyclerView by bindView<RecyclerView>(R.id.list)

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

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_order_services, container, false)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putStringArrayList("pendingChanges",
        pendingChanges?.mapTo(ArrayList(), ServiceMeta::id))
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    with(activity as AppCompatActivity) {
      if (!isInNightMode()) {
        toolbar.setLightStatusBar()
      }
    }
    val lm = LinearLayoutManager(view.context)
    recyclerView.layoutManager = lm
    storedOrder = sharedPrefs.getString(P.ServicesOrder.KEY, null)?.split(",") ?: emptyList()
    val instanceChanges = savedInstanceState?.getStringArrayList("pendingChanges")
    pendingChanges = instanceChanges?.map { serviceMetas[it] as ServiceMeta }
    val displayOrder = instanceChanges ?: storedOrder

    val currentItemsSorted = serviceMetas.values.sortedBy { displayOrder.indexOf(it.id) }
    val adapter = Adapter(
        // Use a day context since this is like the tablayout UI
        recyclerView.context.asDayContext(),
        currentItemsSorted) { newItemOrder ->
      pendingChanges = if (newItemOrder != currentItemsSorted) {
        newItemOrder
      } else {
        null
      }
    }
    recyclerView.adapter = adapter
    savedInstanceState?.getParcelable<Parcelable>("orderServicesState")?.let(
        lm::onRestoreInstanceState)
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
    val callback = MoveCallback { start, end -> adapter.move(start, end) }
    ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

    save.setOnClickListener {
      pendingChanges?.let { changes ->
        sharedPrefs.edit {
          putString(P.ServicesOrder.KEY, changes.joinToString(",", transform = ServiceMeta::id))
        }
      }
      activity?.finish()
    }

    val primaryColor = ContextCompat.getColor(save.context, R.color.colorPrimary)
    val textColor = save.context.resolveAttributeColor(android.R.attr.textColorPrimary)
    syllabus.showIfNeverSeen(P.ServicesOrderSeen.KEY,
        TargetRequest(
            target = {
              FabShowTapTarget(delegateTarget = { TapTarget.forView(save, "", "") },
                  fab = save,
                  title = save.resources.getString(R.string.pref_reorder_services),
                  description = save.resources.getString(
                      R.string.pref_order_services_description))
                  .outerCircleColorInt(primaryColor)
                  .outerCircleAlpha(0.96f)
                  .titleTextColorInt(textColor)
                  .descriptionTextColorInt(ColorUtils.modifyAlpha(textColor, 0.2f))
                  .targetCircleColorInt(textColor)
                  .transparentTarget(true)
                  .drawShadow(true)
                  .id("Save")
                  .apply { fontHelper.getFont()?.let(::textTypeface) }
            },
            postDisplay = save::hide
        ))
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

private class Adapter(
    private val context: Context,
    inputItems: List<ServiceMeta>,
    private val changeListener: (List<ServiceMeta>) -> Unit) : RecyclerView.Adapter<Holder>() {

  private val items = inputItems.toMutableList()

  init {
    setHasStableIds(true)
  }

  fun shuffle() {
    val current = items.toList()
    items.shuffle()
    DiffUtil.calculateDiff(object : Callback() {
      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return current[oldItemPosition].id == items[newItemPosition].id
      }

      override fun getOldListSize() = current.size

      override fun getNewListSize() = items.size

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return current[oldItemPosition] == items[newItemPosition]
      }
    }).dispatchUpdatesTo(this)
    changeListener(items)
  }

  override fun getItemId(position: Int): Long {
    return items[position].id.hashCode().toLong()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    val itemView = LayoutInflater.from(context).inflate(R.layout.order_services_item, parent,
        false)
    return Holder(itemView)
  }

  override fun getItemCount() = items.size

  override fun onBindViewHolder(holder: Holder, position: Int) {
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

private class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val container by bindView<View>(R.id.container)
  private val title by bindView<TextView>(R.id.title)
  private val icon by bindView<ImageView>(R.id.icon)
  private val raise = itemView.resources.getDimensionPixelSize(R.dimen.touch_raise).toFloat()
  private val elevationAnimator = AnimatorInflater.loadStateListAnimator(itemView.context,
      R.animator.raise)

  fun bind(meta: ServiceMeta) {
    title.setText(meta.name)
    container.setBackgroundColor(
        ContextCompat.getColor(title.context, meta.themeColor))
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

private class MoveCallback(
    private val callback: (Int, Int) -> Unit) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
  override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder,
      target: ViewHolder): Boolean {
    callback(viewHolder.adapterPosition, target.adapterPosition)
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
        (it as Holder).updateSelection(true)
      }
    }
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
    super.clearView(recyclerView, viewHolder)
    (viewHolder as Holder).updateSelection(false)
  }
}

@Module
abstract class OrderServicesBindingModule {

  @PerFragment
  @ContributesAndroidInjector(
      modules = [OrderServicesModule::class]
  )
  internal abstract fun orderServicesFragment(): OrderServicesFragment
}

@Module(includes = [ResolvedCatchUpServiceMetaRegistry::class])
abstract class OrderServicesModule {

  @Multibinds
  abstract fun serviceMetas(): Map<String, ServiceMeta>
}

private class FabShowTapTarget(
    private val delegateTarget: () -> TapTarget,
    private val fab: FloatingActionButton,
    title: CharSequence,
    description: CharSequence?
) : TapTarget(title, description) {

  override fun bounds(): Rect {
    val location = IntArray(2)
    fab.getLocationOnScreen(location)
    return Rect(location[0], location[1],
        location[0] + fab.width, location[1] + fab.height)
  }

  override fun onReady(runnable: Runnable) {
    fab.doOnLayout {
      if (fab.isShown) {
        delegateTarget().onReady(runnable)
      } else {
        fab.show(object : OnVisibilityChangedListener() {
          override fun onShown(fab: FloatingActionButton) {
            delegateTarget().onReady(runnable)
          }
        })
      }
    }
  }
}
