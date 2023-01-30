plugins {
  kotlin("jvm")
  alias(libs.plugins.sqldelight)
}

sqldelight {
  databases {
    create("GemojiDatabase") {
      packageName.set("dev.zacsweers.catchup.gemoji.db")
    }
  }
}