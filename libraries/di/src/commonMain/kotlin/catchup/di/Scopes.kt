package catchup.di

import javax.inject.Scope
import kotlin.reflect.KClass

abstract class AppScope private constructor()

// TODO migrate to anvil's
typealias SingleIn = com.squareup.anvil.annotations.optional.SingleIn
