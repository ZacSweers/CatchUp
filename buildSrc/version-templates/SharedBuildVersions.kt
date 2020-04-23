@file:JvmName("SharedBuildVersions")
package dev.zacsweers.catchup.gradle

object SharedBuildVersions {
  const val agp = "$agpVersion"
  const val kotlin = "$kotlinVersion"
  const val okio = "$okioVersion"
  const val moshi = "$moshiVersion"
  val kotlinCompilerArgs = listOf($kotlinCompilerArgs)
  const val kotlinJvmTarget = "$kotlinJvmTarget"
}