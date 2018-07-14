#!/usr/bin/env bash

set -e

# Always delete keys at the end
function cleanup {
  echo "Deleting keys"
  rm -f signing/app-release.jks
  rm -f signing/play-account.p12
}
trap cleanup EXIT

# Keep this for relative paths later
WORKING_DIR=`pwd`

#### CLI options
# -t Can be set to (major|minor|patch) for version updates
VERSION_UPDATE_TYPE=""
# -c Updates changelog with a new cut, off by default
UPDATE_CHANGELOG=false
# -d
DRY_RUN=false
# -v
VERBOSE=false

# Echoes extra logging info
function log {
  if [ "$VERBOSE" = true ]
    then
      echo "--${1}"
  fi
}

# Checks if an env value is present and not empty
function checkEnv {
  echo "Checking"
  env_value=$(printf '%s\n' "${!1}")
  if [ -z ${env_value} ]; then
    echo "$1 is undefined, exiting..."
    exit 1
  else
    log "Found value for $1"
  fi
}

# Helper method to exec gradle and fail the script if Gradle failed
function execGradle {
  gradlew_return_code=0
  ./gradlew $@ --quiet || gradlew_return_code=$?
  if [ ${gradlew_return_code} -ne 0 ]
    then
      echo "Gradle error'd with code ${gradlew_return_code}, exiting."
      exit 1;
  fi
}

# Prints the usage
usage() {
  echo "Usage: $0 [-c] [-d] [-v] [-t <string>]" 1>&2;
  exit 1;
}

# Executes a command if DRY_RUN is not true
function execIfNotDry {
  if [ "$DRY_RUN" = true  ]
    then
      echo "$*"
      return 0
  fi

  eval "$@"
}

# If we don't have these two envs, bomb out early because this won't work
checkEnv CATCHUP_SIGNING_ENCRYPT_KEY
checkEnv CATCHUP_P12_ENCRYPT_KEY

# Read in our CLI args
# blackmagicfuckery
while getopts "cdvt:" o; do
    case "${o}" in
        c)
            UPDATE_CHANGELOG=true
            ;;
        d)
            DRY_RUN=true
            ;;
        v)
            VERBOSE=true
            ;;
        t)
            VERSION_UPDATE_TYPE=${OPTARG}
            ;;
        *)
            usage
            ;;
    esac
done
shift $((OPTIND-1))

log "Received args"
log "Version ${VERSION_UPDATE_TYPE}"
log "Changelog? ${UPDATE_CHANGELOG}"
log "Dry run? ${DRY_RUN}"

#### Version updates
# If version type is not empty...
if [[ ! -z ${VERSION_UPDATE_TYPE} ]]
  then
    # Update the version first. Easiest to do this in gradle because my bash-fu is not great
    echo "Updating version '${VERSION_UPDATE_TYPE}' via gradle..."
    execIfNotDry execGradle :app:updateVersion -updateType=${VERSION_UPDATE_TYPE}
fi

#### Changelog cuts
if [ "$UPDATE_CHANGELOG" = true ]
  then
    echo "Cutting changelog via gradle..."
    execIfNotDry execGradle cutChangelog -PincludeChangelog
    VERSION_NAME=`git describe --abbrev=0 --tags`
    # If we didn't update a version here, use the described tag
    if [ -z ${VERSION_UPDATE_TYPE} ]
      then
        VERSION_NAME=`git describe --tags`
    fi
    echo "Committing new tags for changelog update with version ${VERSION_NAME}"
    # Not using execIfNotDry because args get screwed up and my bash is not good
    if [ "$DRY_RUN" = false  ]
      then
        log "Committing changelog"
        git commit -m "Prepare for release ${VERSION_NAME}." -- CHANGELOG.md app/src/main/play/en-US/whatsnew
    fi
    # Only cut a tag if we didn't update a version above
    if [ -z ${VERSION_UPDATE_TYPE} ]
      then
        log "Cutting tag during changelog"
        execIfNotDry git tag -a ${VERSION_NAME} -m "Version ${VERSION_NAME}."
    fi
fi

echo "Decrypting keys"
# Decrypt keys
execIfNotDry openssl aes-256-cbc -d -in signing/app-release.aes -out signing/app-release.jks -k $CATCHUP_SIGNING_ENCRYPT_KEY
# Decrypt play store key
execIfNotDry openssl aes-256-cbc -d -in signing/play-account.aes -out signing/play-account.p12 -k $CATCHUP_P12_ENCRYPT_KEY
log "Keys decrypted"

echo "Publishing"
execIfNotDry execGradle clean publishApkRelease --no-daemon -PincludeChangelog -PenableFirebasePerf

echo "Finishing up"
execIfNotDry git push
execIfNotDry git push --tags
