package io.sweers.moshkt.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.util.Collections
import java.util.LinkedHashMap
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS


@Retention(RUNTIME)
@Target(CLASS)
annotation class MoshiSerializable

class MoshiSerializableFactory : JsonAdapter.Factory {
  private val adapters = Collections.synchronizedMap(
      LinkedHashMap<Class<*>, Constructor<out JsonAdapter<*>>>())

  override fun create(type: Type, annotations: MutableSet<out Annotation>,
      moshi: Moshi): JsonAdapter<*>? {

    val rawType = Types.getRawType(type)
    if (!rawType.isAnnotationPresent(MoshiSerializable::class.java)) {
      return null
    }

    val constructor = findConstructorForClass(rawType) ?: return null

    try {
      return if (constructor.parameterTypes.size == 1) {
        constructor.newInstance(moshi)
      } else {
        constructor.newInstance(moshi, type)
      }
    } catch (e: IllegalAccessException) {
      throw RuntimeException("Unable to invoke " + constructor, e)
    } catch (e: InstantiationException) {
      throw RuntimeException("Unable to invoke " + constructor, e)
    } catch (e: InvocationTargetException) {
      val cause = e.cause
      if (cause is RuntimeException) {
        throw cause
      }
      if (cause is Error) {
        throw cause
      }
      throw RuntimeException(
          "Could not create generated JsonAdapter instance for type " + rawType, cause)
    }

  }

  private fun findConstructorForClass(cls: Class<*>): Constructor<out JsonAdapter<*>>? {
    var adapterCtor: Constructor<out JsonAdapter<*>>? = adapters[cls]
    if (adapterCtor != null) {
      return adapterCtor
    }
    val clsName = cls.name
    if (clsName.startsWith("android.")
        || clsName.startsWith("java.")
        || clsName.startsWith("kotlin.")) {
      return null
    }
    try {
      val bindingClass = cls.classLoader
          .loadClass(clsName + "_JsonAdapter")
      adapterCtor = try {
        // Try the moshi constructor
        @Suppress("UNCHECKED_CAST")
        bindingClass.getConstructor(
            Moshi::class.java) as Constructor<out JsonAdapter<*>>
      } catch (e: NoSuchMethodException) {
        // Try the moshi + type constructor
        @Suppress("UNCHECKED_CAST")
        bindingClass.getConstructor(Moshi::class.java,
            Type::class.java) as Constructor<out JsonAdapter<*>>
      }

    } catch (e: ClassNotFoundException) {
      adapterCtor = findConstructorForClass(cls.superclass)
    } catch (e: NoSuchMethodException) {
      throw RuntimeException("Unable to find binding constructor for " + clsName, e)
    }

    adapters[cls] = adapterCtor
    return adapterCtor
  }

  companion object {
    private var instance: WeakReference<MoshiSerializableFactory>? = null

    fun getInstance() =
        instance?.get() ?: MoshiSerializableFactory().also { instance = WeakReference(it) }
  }
}
