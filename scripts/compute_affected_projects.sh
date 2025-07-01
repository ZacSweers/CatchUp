#!/usr/bin/env bash

project_root="$(git rev-parse --show-toplevel)"
SERIALIZED_BUILD_GRAPH="${project_root}/.cache/dependencyGraph/serializedGraph.bin"
CHANGED_FILES="tmp/changed_files.txt"

./scripts/parse-dependency-graph.main.kts --output-file "${SERIALIZED_BUILD_GRAPH}" --compute-graph --verbose

./scripts/skippy.main.kts \
    --changed-files "${CHANGED_FILES}" \
    --dependency-graph "${SERIALIZED_BUILD_GRAPH}" \
    --outputs-dir build/skippy \
    --merge-outputs \
    --debug \
    --root-dir "${project_root}" \
    --config config/skippy/config.json