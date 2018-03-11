package io.sweers.moshkt.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(RUNTIME)
@Target(CLASS)
annotation class MoshiSerializable

class MoshiSerializableFactory : JsonAdapter.Factory {

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {

    val rawType = Types.getRawType(type)
    if (!rawType.isAnnotationPresent(MoshiSerializable::class.java)) {
      return null
    }

    val clsName = rawType.name.replace("$", "_")
    val constructor = try {
      @Suppress("UNCHECKED_CAST")
      val bindingClass = rawType.classLoader
          .loadClass(clsName + "JsonAdapter") as Class<out JsonAdapter<*>>
      if (type is ParameterizedType) {
        // This is generic, use the two param moshi + type constructor
        bindingClass.getDeclaredConstructor(Moshi::class.java, Array<Type>::class.java)
      } else {
        // The standard single param moshi constructor
        bindingClass.getDeclaredConstructor(Moshi::class.java)
      }
    } catch (e: ClassNotFoundException) {
      throw RuntimeException("Unable to find generated Moshi adapter class for $clsName", e)
    } catch (e: NoSuchMethodException) {
      throw RuntimeException("Unable to find generated Moshi adapter constructor for $clsName", e)
    }

    try {
      return when {
        constructor.parameterTypes.size == 1 -> constructor.newInstance(moshi)
        type is ParameterizedType -> constructor.newInstance(moshi, type.actualTypeArguments)
        else -> throw IllegalStateException("Unable to handle type $type")
      }
    } catch (e: IllegalAccessException) {
      throw RuntimeException("Unable to invoke $constructor", e)
    } catch (e: InstantiationException) {
      throw RuntimeException("Unable to invoke $constructor", e)
    } catch (e: InvocationTargetException) {
      val cause = e.cause
      if (cause is RuntimeException) {
        throw cause
      }
      if (cause is Error) {
        throw cause
      }
      throw RuntimeException(
          "Could not create generated JsonAdapter instance for type $rawType", cause)
    }
  }
}
