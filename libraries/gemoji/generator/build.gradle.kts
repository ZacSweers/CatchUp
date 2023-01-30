plugins {
  kotlin("jvm")
  `application`
  alias(libs.plugins.moshix)
}

application {
  mainClass.set("dev.zacsweers.catchup.gemoji.generator.MainKt")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.sqldelight.driver.jvm)
  implementation(projects.libraries.gemoji.db)
}