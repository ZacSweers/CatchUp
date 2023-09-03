import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.sqldelight)
}

kotlin {
  // region KMP Targets
  jvm()
  // endregion

  @OptIn(ExperimentalKotlinGradlePluginApi::class) targetHierarchy.default()
}

sqldelight {
  databases {
    create("GemojiDatabase") {
      packageName.set("catchup.gemoji.db")
      schemaOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/databases"))
      migrationOutputDirectory.set(layout.projectDirectory.dir("src/main/sqldelight/migrations"))
    }
  }
}
