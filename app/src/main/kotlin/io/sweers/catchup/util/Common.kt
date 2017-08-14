package io.sweers.catchup.util

/**
 * Applies a [block] on a set of [args].
 */
inline fun <T> applyOn(vararg args: T, crossinline block: T.() -> Unit) {
  args.asSequence().onEach { block(it) }
}
