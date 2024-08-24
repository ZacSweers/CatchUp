package catchup.di

import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass

abstract class AppScope private constructor()

@Scope @Retention(RUNTIME) annotation class SingleIn(val scope: KClass<*>)
