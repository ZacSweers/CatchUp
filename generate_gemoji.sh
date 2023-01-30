#!/usr/bin/env sh

# TODO make this download the latest file too + format it?
./gradlew :libraries:gemoji:generator:run --args="--json $PWD/libraries/gemoji/generator/gemoji.json --db $PWD/libraries/gemoji/src/main/assets/databases/gemoji.db"