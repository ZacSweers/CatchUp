package catchup.di

import javax.inject.Qualifier

// TODO split offline out into a separate mode
enum class DataMode {
  REAL,
  OFFLINE,
  FAKE,
}

@Qualifier annotation class FakeMode
