package io.sweers.catchup.serviceregistry.annotations

import com.uber.crumb.annotations.CrumbConsumer
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

@CrumbConsumer
@Target(CLASS)
@Retention(BINARY)
annotation class ServiceRegistry
