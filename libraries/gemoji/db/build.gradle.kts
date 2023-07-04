plugins {
  kotlin("jvm")
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.sqldelight)
}

sqldelight {
  databases {
    create("GemojiDatabase") {
      packageName.set("dev.zacsweers.catchup.gemoji.db")
    }
  }
}
