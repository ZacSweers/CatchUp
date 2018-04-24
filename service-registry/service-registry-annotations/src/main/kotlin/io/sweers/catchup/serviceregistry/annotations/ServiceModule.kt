package io.sweers.catchup.serviceregistry.annotations

import com.uber.crumb.annotations.CrumbProducer
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

@CrumbProducer
@Target(CLASS)
@Retention(BINARY)
annotation class ServiceModule
