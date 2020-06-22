tzdata
======

This project contains generated granular timezone `.dat` files use with
`LazyZoneRulesProvider`.

Note that in order to enable this, the following property should be set in the consuming app:

```kotlin
System.setProperty(
	"java.time.zone.DefaultZoneRulesProvider",
	"com.gabrielittner.threetenbp.LazyZoneRulesProvider"
)
```

Source: https://github.com/google/desugar_jdk_libs/blob/c3f88ccba469a041961dbb3fbbf5bf04962cecbd/src/share/classes/java/time/zone/TzdbZoneRulesProvider.java#L110

To regenerate data with a new version, update the `tzVersion` in the `tickTock` extension in
`build.gradle.kts` and run `./gradlew generateLazyZoneRules`.
