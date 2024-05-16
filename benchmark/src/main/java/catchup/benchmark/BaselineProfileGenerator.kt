// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package catchup.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
  @get:Rule val baselineProfileRule = BaselineProfileRule()

  @Test
  fun startupBaselineProfile() =
    baselineProfileRule.collect(
      packageName = "dev.zacsweers.catchup",
      // Iteration values recommended by AndroidX folks
      maxIterations = 15,
      stableIterations = 3,
      includeInStartupProfile = true,
      profileBlock = { startActivityAndWait() },
    )
}
