package catchup.di

import javax.inject.Scope
import kotlin.reflect.KClass

abstract class AppScope private constructor()

@Scope annotation class SingleIn(val scope: KClass<*>)
