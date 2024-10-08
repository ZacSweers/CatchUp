import foundry.gradle.Platforms

plugins {
  alias(libs.plugins.foundry.base)
  `java-platform`
}

val catalogExtension =
  extensions.findByType<VersionCatalogsExtension>() ?: error("Could not find any version catalogs!")

for (name in catalogExtension.catalogNames) {
  val catalog = catalogExtension.named(name)
  Platforms.applyFromCatalog(project, catalog)
}