plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(deps.android.build.compileSdkVersion)
  buildToolsVersion(deps.android.build.buildToolsVersion)

  defaultConfig {
    minSdkVersion(deps.android.build.minSdkVersion)
    targetSdkVersion(deps.android.build.targetSdkVersion)
  }
}

dependencies {
  implementation(deps.android.support.annotations)
  implementation(deps.kotlin.stdlib.jdk7)
}
