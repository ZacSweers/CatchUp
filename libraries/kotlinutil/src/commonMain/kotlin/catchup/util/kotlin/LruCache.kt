package catchup.util.kotlin

import catchup.util.kotlin.NullableLruCache.Optional.None
import catchup.util.kotlin.NullableLruCache.Optional.Some

class LruCache<K : Any, V : Any>(private val cacheSize: Int) :
  LinkedHashMap<K, V>(
    cacheSize,
    // NOTE using named arguments here breaks the compiler
    0.75f,
    true,
  ) {

  init {
    require(cacheSize >= 0)
  }

  override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
    return size > cacheSize
  }
}

class NullableLruCache<K : Any, V : Any>(maxSize: Int) {
  @PublishedApi internal val delegate = LruCache<K, Optional<V>>(maxSize)

  inline fun computeIfAbsent(key: K, create: () -> V?): V? {
    return when (val cached = delegate[key]) {
      is Some -> cached.value
      is None -> null
      null -> create().also { delegate[key] = if (it == null) None else Some(it) }
    }
  }

  @PublishedApi
  internal sealed class Optional<out T> {
    data class Some<out T : Any>(val value: T) : Optional<T>()

    data object None : Optional<Nothing>()
  }
}
