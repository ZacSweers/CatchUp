package io.sweers.catchup.serviceregistry.annotations

import com.uber.crumb.annotations.CrumbQualifier
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

@CrumbQualifier
@Target(CLASS)
@Retention(BINARY)
annotation class Meta
