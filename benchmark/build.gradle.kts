// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  id("com.android.test")
  kotlin("android")
  alias(libs.plugins.sgp.base)
  alias(libs.plugins.baselineprofile)
}

val mvdName = "pixel6Api31"

android {
  namespace = "io.sweers.catchup.benchmark"
  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>(mvdName) {
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
  managedDevices += mvdName

  // This enables using connected devices to generate profiles. The default is true.
  // When using connected devices, they must be rooted or API 33 and higher.
  useConnectedDevices = false

  // Set to true to see the emulator, useful for debugging. Only enabled locally
  enableEmulatorDisplay = false
}

dependencies {
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.uiautomator)
}
