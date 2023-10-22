plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.sqldelight)
}

kotlin {
  // region KMP Targets
  jvm()
  // endregion

  applyDefaultHierarchyTemplate()
}

sqldelight {
  databases {
    create("GemojiDatabase") {
      packageName.set("catchup.gemoji.db")
      schemaOutputDirectory.set(layout.projectDirectory.dir("src/commonMain/sqldelight/databases"))
      migrationOutputDirectory.set(layout.projectDirectory.dir("src/commonMain/sqldelight/migrations"))
    }
  }
}
