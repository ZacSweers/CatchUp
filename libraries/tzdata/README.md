tzdata
======

This project contains Gradle scripts that generate a `tzdb.dat` file for use with AGP library desugaring.

If enabled, the desugared libraries will look for a `tzdb.dat` file at `j$/time/zone` in resources.

Note that in order to enable this, the following property should be set in the consuming app:

```kotlin
System.setProperty(
	"java.time.zone.DefaultZoneRulesProvider",
	"j$.time.zone.TzdbZoneRulesProvider"
)
```

Source: https://github.com/google/desugar_jdk_libs/blob/c3f88ccba469a041961dbb3fbbf5bf04962cecbd/src/share/classes/java/time/zone/TzdbZoneRulesProvider.java#L110
