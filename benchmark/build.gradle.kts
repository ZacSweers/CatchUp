// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.ManagedVirtualDevice
import foundry.gradle.isCi

plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.foundry.base)
  alias(libs.plugins.baselineprofile)
}

val mvdApi = 33
val mvdName = "pixel6Api$mvdApi"

android {
  namespace = "catchup.benchmark"
  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions.managedDevices.devices {
    create<ManagedVirtualDevice>(mvdName) {
      device = "Pixel 6"
      apiLevel = mvdApi
      // Google source can run both arm and x86 binaries
      systemImageSource = "google"
      require64Bit = true
    }
  }

  targetProjectPath = ":app"
  // Load the target app in a separate process so that it can be restarted multiple times, which
  // is necessary for startup benchmarking to work correctly.
  // https://source.android.com/docs/core/tests/development/instr-self-e2e
  experimentalProperties["android.experimental.self-instrumenting"] = true
  experimentalProperties["android.experimental.testOptions.managedDevices.setupTimeoutMinutes"] = 20
  experimentalProperties["android.experimental.androidTest.numManagedDeviceShards"] = 1
  experimentalProperties["android.experimental.testOptions.managedDevices.maxConcurrentDevices"] = 1
  experimentalProperties[
    "android.experimental.testOptions.managedDevices.emulator.showKernelLogging"] = true
  if (isCi) {
    experimentalProperties["android.testoptions.manageddevices.emulator.gpu"] = "swiftshader_indirect"
  }
}

val useConnectedDevice =
  providers.gradleProperty("catchup.benchmark.useConnectedDevice").getOrElse("false").toBoolean()

baselineProfile {
  // This specifies the managed devices to use that you run the tests on. The
  // default is none.
  if (!useConnectedDevice) {
    managedDevices += mvdName
  }

  // This enables using connected devices to generate profiles. The default is
  // true. When using connected devices, they must be rooted or API 33 and
  // higher.
  useConnectedDevices = useConnectedDevice

  // Disable the emulator display for GMD devices on CI
  enableEmulatorDisplay = !isCi
}

dependencies {
  implementation(libs.androidx.benchmark.macro.junit)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.uiautomator)
}
