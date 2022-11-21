package dev.zacsweers.catchup.di

import javax.inject.Scope
import kotlin.reflect.KClass

abstract class AppScope private constructor()

abstract class ActivityScope private constructor()

@Scope annotation class SingleIn(val scope: KClass<*>)

@Scope annotation class ForScope(val scope: KClass<*>)
