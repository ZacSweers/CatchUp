#!/usr/bin/env bash
set -uo pipefail

./gradlew rakeDependencies aggregateMissingIdentifiers -Pslack.gradle.config.enableAnalysisPlugin=true --no-configuration-cache

bold=$(tput bold)
red=$(tput setaf 1)
reset=$(tput sgr0)

if [[ -e build/rake/aggregated_missing_identifiers.txt ]] && [[ -s build/rake/aggregated_missing_identifiers.txt ]]; then
    echo "${red}${bold}Error: Missing identifiers found at $(pwd)/build/rake/aggregated_missing_identifiers.txt. Please add them to libs.versions.toml or create dependency bundles.${reset}"
    # TODO eventually exit early?
fi

./gradlew sortDependencies
./gradlew spotlessApply