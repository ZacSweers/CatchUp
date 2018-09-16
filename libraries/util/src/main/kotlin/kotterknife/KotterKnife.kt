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
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/*
 * The age-old kotterknife impl with some adjustments
 * - ViewBindable interface for custom things that can look up views, like viewholder classes or controllers
 *   - Basically lets you do like ButterKnife.bind(target, source)
 * - onClick helpers
 * - Allow for Any return type to account for interfaces, matching ButterKnife's support
 * - LazyBinding updated to match more modern Lazy implementation
 */

/**
 * Implementers of this can provide a view given a [viewFinder].
 */
interface ViewBindable {
  /**
   * @returns a view finder that can locate a view with a given resource ID parameter.
   */
  val viewFinder: (Int) -> Any?
}

/**
 * Implementation of a [ViewBindable] for objects that delegate to a source [View], similar to a
 * [ViewHolder].
 */
abstract class ViewDelegateBindable(source: View) : ViewBindable {
  final override val viewFinder: (Int) -> Any? = { source.findViewById(it) }
}

fun <V : Any> ViewBindable.bindView(id: Int)
    : ReadOnlyProperty<ViewBindable, V> = required(id, viewFinder)

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

fun <V : Any> View.bindView(id: Int)
    : ReadOnlyProperty<View, V> = required(id, viewFinder)

fun <V : Any> Activity.bindView(id: Int)
    : ReadOnlyProperty<Activity, V> = required(id, viewFinder)

fun <V : Any> Dialog.bindView(id: Int)
    : ReadOnlyProperty<Dialog, V> = required(id, viewFinder)

fun <V : Any> DialogFragment.bindView(id: Int)
    : ReadOnlyProperty<DialogFragment, V> = required(id, viewFinder)

fun <V : Any> Fragment.bindView(id: Int)
    : ReadOnlyProperty<Fragment, V> = required(id, viewFinder)

fun <V : Any> ViewHolder.bindView(id: Int)
    : ReadOnlyProperty<ViewHolder, V> = required(id, viewFinder)

fun <V : Any> ViewBindable.bindOptionalView(id: Int)
    : ReadOnlyProperty<ViewBindable, V?> = optional(id, viewFinder)

fun <V : Any> View.bindOptionalView(id: Int)
    : ReadOnlyProperty<View, V?> = optional(id, viewFinder)

fun <V : Any> Activity.bindOptionalView(id: Int)
    : ReadOnlyProperty<Activity, V?> = optional(id, viewFinder)

fun <V : Any> Dialog.bindOptionalView(id: Int)
    : ReadOnlyProperty<Dialog, V?> = optional(id, viewFinder)

fun <V : Any> DialogFragment.bindOptionalView(id: Int)
    : ReadOnlyProperty<DialogFragment, V?> = optional(id, viewFinder)

fun <V : Any> Fragment.bindOptionalView(id: Int)
    : ReadOnlyProperty<Fragment, V?> = optional(id, viewFinder)

fun <V : Any> ViewHolder.bindOptionalView(id: Int)
    : ReadOnlyProperty<ViewHolder, V?> = optional(id, viewFinder)

fun <V : Any> ViewBindable.bindViews(vararg ids: Int)
    : ReadOnlyProperty<ViewBindable, List<V>> = required(ids, viewFinder)

fun <V : Any> View.bindViews(vararg ids: Int)
    : ReadOnlyProperty<View, List<V>> = required(ids, viewFinder)

fun <V : Any> Activity.bindViews(vararg ids: Int)
    : ReadOnlyProperty<Activity, List<V>> = required(ids, viewFinder)

fun <V : Any> Dialog.bindViews(vararg ids: Int)
    : ReadOnlyProperty<Dialog, List<V>> = required(ids, viewFinder)

fun <V : Any> DialogFragment.bindViews(vararg ids: Int)
    : ReadOnlyProperty<DialogFragment, List<V>> = required(ids, viewFinder)

fun <V : Any> Fragment.bindViews(vararg ids: Int)
    : ReadOnlyProperty<Fragment, List<V>> = required(ids, viewFinder)

fun <V : Any> ViewHolder.bindViews(vararg ids: Int)
    : ReadOnlyProperty<ViewHolder, List<V>> = required(ids, viewFinder)

fun <V : Any> ViewBindable.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<ViewBindable, List<V>> = optional(ids, viewFinder)

fun <V : Any> View.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<View, List<V>> = optional(ids, viewFinder)

fun <V : Any> Activity.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<Activity, List<V>> = optional(ids, viewFinder)

fun <V : Any> Dialog.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<Dialog, List<V>> = optional(ids, viewFinder)

fun <V : Any> DialogFragment.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<DialogFragment, List<V>> = optional(ids, viewFinder)

fun <V : Any> Fragment.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<Fragment, List<V>> = optional(ids, viewFinder)

fun <V : Any> ViewHolder.bindOptionalViews(vararg ids: Int)
    : ReadOnlyProperty<ViewHolder, List<V>> = optional(ids, viewFinder)

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
    finder: (Int) -> Any?) = LazyBinding<T, V> { desc ->
  finder(id) as V? ?: viewNotFound(id, desc)
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> optional(id: Int, finder: (Int) -> Any?) = LazyBinding<T, V?> {
  finder(id) as V?
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> required(ids: IntArray,
    finder: (Int) -> Any?) = LazyBinding<T, List<V>> { desc ->
  ids.map {
    finder(it) as V? ?: viewNotFound(it, desc)
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T, V : Any> optional(ids: IntArray,
    finder: (Int) -> Any?) = LazyBinding<T, List<V>> { _ ->
  ids.map {
    finder(it) as V?
  }.filterNotNull()
}

private object EMPTY

// Like Kotlin's lazy delegate but the initializer gets the target and metadata passed to it
private class LazyBinding<T, V>(initializer: ((KProperty<*>) -> V)) : ReadOnlyProperty<T, V> {
  private var initializer: ((KProperty<*>) -> V)? = initializer
  private var value: Any? = EMPTY

  override fun getValue(thisRef: T, property: KProperty<*>): V {
    if (value === EMPTY) {
      value = initializer!!(property)
      initializer = null
    }
    @Suppress("UNCHECKED_CAST")
    return value as V
  }

  override fun toString(): String = if (value !== EMPTY) value.toString() else "LazyBinding value not initialized yet."
}
