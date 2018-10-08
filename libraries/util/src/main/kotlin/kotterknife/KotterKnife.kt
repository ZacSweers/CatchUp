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

package kotterknife

import android.app.Activity
import android.app.Dialog
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.util.WeakHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/*
 * The age-old kotterknife impl with some adjustments
 * - ViewBindable interface for custom things that can look up views, like viewholder classes
 *   - Basically lets you do like ButterKnife.bind(target, source)
 * - onClick helpers
 * - Allow for Any return type to account for interfaces, matching ButterKnife's support
 * - Optional onBound callbacks for inits
 */

/**
 * Implementers of this can provide a view given a [viewFinder].
 */
interface ViewBindable {
  /**
   * @returns a view finder that can locate a view with a given resource ID parameter.
   */
  val viewFinder: (resId: Int) -> Any?
}

/**
 * Implementation of a [ViewBindable] for objects that delegate to a source [View], similar to a
 * [ViewHolder].
 */
abstract class ViewDelegateBindable(source: View) : ViewBindable {
  final override val viewFinder: (resId: Int) -> Any? = { source.findViewById(it) }
}

fun <V : Any> ViewBindable.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<ViewBindable, V> = required(id, viewFinder, onBound)

fun <V : Any> Fragment.onClick(id: Int, body: (V) -> Unit) {
  (viewFinder(id))?.setOnClickListener {
    @Suppress("UNCHECKED_CAST")
    body(it as V)
  } ?: viewNotFound(id)
}

fun <V : Any> ViewBindable.onClick(id: Int, body: (V) -> Unit) {
  (viewFinder(id) as? View)?.setOnClickListener {
    @Suppress("UNCHECKED_CAST")
    body(it as V)
  } ?: viewNotFound(id)
}

fun <V : Any> View.onSubviewClick(id: Int, body: (V) -> Unit) {
  viewFinder(id)?.setOnClickListener {
    @Suppress("UNCHECKED_CAST")
    body(it as V)
  } ?: viewNotFound(id)
}

fun <V : Any> View.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<View, V> = required(id, viewFinder, onBound)

fun <V : Any> Activity.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<Activity, V> = required(id, viewFinder, onBound)

fun <V : Any> Dialog.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<Dialog, V> = required(id, viewFinder, onBound)

fun <V : Any> DialogFragment.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<DialogFragment, V> = required(id, viewFinder, onBound)

fun <V : Any> Fragment.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<Fragment, V> = required(id, viewFinder, onBound)

fun <V : Any> ViewHolder.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<ViewHolder, V> = required(id, viewFinder, onBound)

fun <V : Any> ViewBindable.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<ViewBindable, V?> = optional(id, viewFinder, onBound)

fun <V : Any> View.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<View, V?> = optional(id, viewFinder, onBound)

fun <V : Any> Activity.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<Activity, V?> = optional(id, viewFinder, onBound)

fun <V : Any> Dialog.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<Dialog, V?> = optional(id, viewFinder, onBound)

fun <V : Any> DialogFragment.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<DialogFragment, V?> = optional(id, viewFinder, onBound)

fun <V : Any> Fragment.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<Fragment, V?> = optional(id, viewFinder, onBound)

fun <V : Any> ViewHolder.bindOptionalView(id: Int,
    onBound: ((V?) -> Unit)? = null)
    : ReadOnlyProperty<ViewHolder, V?> = optional(id, viewFinder, onBound)

fun <V : Any> ViewBindable.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<ViewBindable, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> View.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<View, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> Activity.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<Activity, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> Dialog.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<Dialog, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> DialogFragment.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<DialogFragment, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> Fragment.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<Fragment, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> ViewHolder.bindViews(vararg ids: Int,
    onBound: ((List<V>) -> Unit)? = null)
    : ReadOnlyProperty<ViewHolder, List<V>> = required(ids, viewFinder, onBound)

fun <V : Any> ViewBindable.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<ViewBindable, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> View.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<View, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> Activity.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<Activity, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> Dialog.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<Dialog, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> DialogFragment.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<DialogFragment, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> Fragment.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<Fragment, List<V>> = optional(ids, viewFinder, onBound)

fun <V : Any> ViewHolder.bindOptionalViews(vararg ids: Int,
    onBound: ((List<V?>) -> Unit)? = null)
    : ReadOnlyProperty<ViewHolder, List<V>> = optional(ids, viewFinder, onBound)

private val View.viewFinder: (Int) -> View?
  get() = { findViewById(it) }
private val Activity.viewFinder: (Int) -> View?
  get() = { findViewById(it) }
private val Dialog.viewFinder: (Int) -> View?
  get() = { findViewById(it) }
private val DialogFragment.viewFinder: (Int) -> View?
  get() = { dialog?.findViewById(it) ?: view?.findViewById(it) }
private val Fragment.viewFinder: (Int) -> View?
  get() = { view!!.findViewById(it) }
private val ViewHolder.viewFinder: (Int) -> View?
  get() = { itemView.findViewById(it) }

private fun viewNotFound(id: Int, desc: KProperty<*>? = null): Nothing {
  if (desc == null) {
    throw IllegalStateException("View ID $id not found.")
  } else {
    throw IllegalStateException("View ID $id for '${desc.name}' not found.")
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> required(id: Int,
    finder: (Int) -> Any?,
    onBound: ((V) -> Unit)? = null) = LazyBinding<T, V>(onBound) { desc ->
  finder(id) as V? ?: viewNotFound(id, desc)
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> optional(id: Int,
    finder: (Int) -> Any?,
    onBound: ((V?) -> Unit)? = null) = LazyBinding<T, V?>(onBound) {
  finder(id) as V?
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> required(ids: IntArray,
    finder: (Int) -> Any?,
    onBound: ((List<V>) -> Unit)? = null) = LazyBinding<T, List<V>>(onBound) { desc ->
  ids.map {
    finder(it) as V? ?: viewNotFound(it, desc)
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> optional(ids: IntArray,
    finder: (Int) -> Any?,
    onBound: ((List<V>) -> Unit)? = null) = LazyBinding<T, List<V>>(onBound) { _ ->
  ids.map {
    finder(it) as V?
  }.filterNotNull()
}

private object EMPTY

// Like Kotlin's lazy delegate but the initializer gets the target and metadata passed to it
private class LazyBinding<T, V>(private var onBound: ((V) -> Unit)? = null,
    private val initializer: ((KProperty<*>) -> V)) : ReadOnlyProperty<T, V> {
  private var value: Any? = EMPTY

  override fun getValue(thisRef: T, property: KProperty<*>): V {
    if (value === EMPTY) {
      value = initializer(property)?.also {
        onBound?.invoke(it)
        LazyRegistry.register(thisRef!!, this)
      }
    }
    @Suppress("UNCHECKED_CAST")
    return value as V
  }

  internal fun reset() {
    value = EMPTY
  }

  override fun toString(): String = if (value !== EMPTY) value.toString() else "LazyBinding value not initialized yet."
}

private object LazyRegistry {
  private val lazyMap = WeakHashMap<Any, MutableCollection<LazyBinding<*, *>>>()

  fun register(target: Any, lazy: LazyBinding<*, *>) {
    lazyMap.getOrPut(target, ::WeakHashSet).add(lazy)
  }

  fun reset(target: Any) {
    lazyMap.remove(target)?.forEach(LazyBinding<*, *>::reset)
  }
}

object KotterKnife {
  fun reset(target: Any) {
    LazyRegistry.reset(target)
  }
}
