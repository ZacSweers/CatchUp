#!/bin/bash
set -e

./scripts/shell-sentry.main.kts --debug --verbose --config config/shell-sentry/config.json -- "$*"
