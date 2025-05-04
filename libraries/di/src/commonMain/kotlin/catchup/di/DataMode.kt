package catchup.di

import dev.zacsweers.metro.Qualifier

// TODO split offline out into a separate mode
enum class DataMode {
  REAL,
  OFFLINE,
  FAKE,
}

@Qualifier annotation class FakeMode
