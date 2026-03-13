plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.foundry.base)
  application
  alias(libs.plugins.sqldelight)
  alias(libs.plugins.moshix)
}

application { mainClass.set("catchup.gemoji.generator.MainKt") }

sqldelight {
  databases {
    create("GemojiDatabase") {
      // Must be set before dependencies
      packageName.set("catchup.gemoji.db.mutable")
      dependency(project(":libraries:gemoji:db"))
      schemaOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/databases"))
      migrationOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/migrations"))
    }
  }
}

dependencies {
  implementation(project(":libraries:gemoji:db"))
  implementation(libs.clikt)
  implementation(libs.sqldelight.driver.jvm)
}
