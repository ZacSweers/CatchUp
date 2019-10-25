/*
 * Copyright (c) 2014 Jake Wharton
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
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
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

fun <V : Any> Fragment.onClick(id: Int, body: (V) -> Unit) {
  (viewFinder(id))?.setOnClickListener {
    @Suppress("UNCHECKED_CAST")
    body(it as V)
  } ?: viewNotFound(id)
}

@CheckResult
fun <T : ViewBinding> Activity.setContentView(
    inflate: (LayoutInflater) -> T
): T = inflate(layoutInflater).also {
  setContentView(it.root)
}

fun <V : Any> Fragment.bindView(id: Int,
    onBound: ((V) -> Unit)? = null)
    : ReadOnlyProperty<Fragment, V> = required(id, viewFinder, onBound)

private val Fragment.viewFinder: (Int) -> View?
  get() = { view!!.findViewById(it) }

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
