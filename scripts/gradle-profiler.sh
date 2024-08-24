#!/usr/bin/env bash

# Optionally allow using a snapshot of gradle-profiler built from source.
# This is useful because sometimes they take a long time to release.
if [[ "$GRADLE_PROFILER_USE_SNAPSHOT" = "true" ]]; then SNAPSHOT=true; fi
ORIGINAL_DIR=$PWD

if [[ ${SNAPSHOT} ]]; then
    echo "Building gradle-profiler from source"

    CLONE_DIR=".cache/gradle-profiler/snapshot"
    rm -rf $CLONE_DIR
    mkdir -p $CLONE_DIR
    cd $CLONE_DIR
    git clone --depth=1 https://github.com/gradle/gradle-profiler.git
    cd gradle-profiler
    ./gradlew installDist
    PROFILER_BIN=$PWD/build/install/gradle-profiler/bin/gradle-profiler
    cd $ORIGINAL_DIR
else
    VERSION=0.20.0
    echo "Using gradle-profiler $VERSION"
    PROFILER_DIR=".cache/gradle-profiler/$VERSION"
    PROFILER_BIN="$PROFILER_DIR/gradle-profiler-$VERSION/bin/gradle-profiler"

    # Download if not present
    if [[ ! -f "$PROFILER_BIN" ]]; then
      PROFILER_ZIP="$PROFILER_DIR/profiler.zip"
      echo "Downloading prebuilt"
      mkdir -p $PROFILER_DIR
      wget --quiet --output-document=$PROFILER_ZIP "https://repo1.maven.org/maven2/org/gradle/profiler/gradle-profiler/${VERSION}/gradle-profiler-${VERSION}.zip"
      unzip -q -o $PROFILER_ZIP -d "$PROFILER_DIR"
      rm $PROFILER_ZIP
    fi
fi

echo "Killing any running daemons or java processes"
./gradlew --stop

# Exec it
# Scan is logged in profile-out/profile.log
$PROFILER_BIN --benchmark --profile buildscan --scenario-file config/gradle/benchmark.scenarios --measure-gc --measure-local-build-cache -Dorg.gradle.daemon.gc.polling.disabled=true "$@"