#!/usr/bin/env bash
set -uo pipefail

./gradlew rakeDependencies aggregateMissingIdentifiers -Pslack.gradle.config.enableAnalysisPlugin=true --no-configuration-cache

if [[ -e build/rake/aggregated_missing_identifiers.txt ]] && [[ -s build/rake/aggregated_missing_identifiers.txt ]]; then
    echo "Error: Missing identifiers found at $(pwd)/build/rake/aggregated_missing_identifiers.txt."
    exit 1
fi

./gradlew sortDependencies
./gradlew spotlessApply