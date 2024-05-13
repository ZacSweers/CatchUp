// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package catchup.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun startup() =
    benchmarkRule.measureRepeated(
      packageName = "dev.zacsweers.catchup",
      metrics = listOf(StartupTimingMetric()),
      iterations = 5,
      startupMode = StartupMode.COLD,
    ) {
      pressHome()
      startActivityAndWait()
    }
}
