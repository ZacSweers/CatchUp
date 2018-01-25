MoshKt
======

Rhymes with moshpit

An annotation processor that reads Kotlin data classes and generates Moshi JsonAdapters for them.
This generates Kotlin code, and understands basic Kotlin language features like default values
and companion objects.

The generated class will match the visibility of the given data class (i.e. if it's internal, the
adapter will also be internal).

If you define a companion object, a jsonAdapter() extension function will be generated onto it.
If you don't want this though, you can use the runtime [MoshiSerializable] factory implementation.

Things that are implemented:
  * Standard, parameterized, and wildcard types
  * Read/write
  * Delegation to moshi instance
  * Companion object extension functions
  * Kotlin nullability
  * Default values (presence only, not actionable yet)

Things that are not implemented yet:
  * Generics support
  * Leveraging default values on params where possible. This might not be feasible though.
  * Custom constructors
