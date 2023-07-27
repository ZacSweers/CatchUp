// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  id("com.android.test")
  kotlin("android")
  alias(libs.plugins.baselineprofile)
}

// TODO fix these in SGP
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.get().toInt())
}

android {
  compileSdk = 34 // TODO add in SGP
  namespace = "io.sweers.catchup.benchmark"
  defaultConfig {
    minSdk = 28 // TODO add in SGP
    targetSdk = 34
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>("pixel6Api31") {
      device = "Pixel 6"
      apiLevel = 31
      systemImageSource = "aosp"
    }
  }

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.r8.dex-startup-optimization"] = true
}

baselineProfile {
  // This specifies the managed devices to use that you run the tests on. The default
  // is none.
  managedDevices += "pixel6Api31"

  // This enables using connected devices to generate profiles. The default is true.
  // When using connected devices, they must be rooted or API 33 and higher.
  useConnectedDevices = false

  // Set to true to see the emulator, useful for debugging. Only enabled locally
  enableEmulatorDisplay = false
}

dependencies {
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.profileinstaller)
}
