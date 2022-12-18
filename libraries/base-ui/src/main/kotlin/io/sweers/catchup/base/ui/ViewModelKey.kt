package io.sweers.catchup.base.ui

import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * A Dagger multi-binding key used for registering a [ViewModel] into the top level dagger graphs.
 */
@MapKey annotation class ViewModelKey(val value: KClass<out ViewModel>)
