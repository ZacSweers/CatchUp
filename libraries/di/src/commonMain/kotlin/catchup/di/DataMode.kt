package catchup.di

import javax.inject.Qualifier

// TODO split offline out into a separate mode
enum class DataMode {
  REAL,
  OFFLINE,
  FAKE,
}

fun interface ModeDependentFactory<T> {
  fun create(mode: DataMode): T
}

@Qualifier annotation class FakeMode
