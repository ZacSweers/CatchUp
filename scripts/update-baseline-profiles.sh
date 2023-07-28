#!/usr/bin/env bash
set -uo pipefail

# If on CI, add indirect swiftshader arg
gpu_arg=" "
if [[ "${CI:-}" == "true" ]]; then
  gpu_arg="-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"
fi

# TODO --unused-only flag for cleaning can't be used because devices appear to be left in a bad state
./gradlew cleanManagedDevices && ./gradlew generateBaselineProfile $gpu_arg