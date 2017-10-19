#!/bin/bash

echo "Decrypting keys"
# Decrypt keys
openssl aes-256-cbc -d -in signing/app-release.aes -out signing/app-release.jks -k $CATCHUP_SIGNING_ENCRYPT_KEY
# Decrypt play store key
openssl aes-256-cbc -d -in signing/play-account.aes -out signing/play-account.p12 -k $CATCHUP_P12_ENCRYPT_KEY
echo "Keys decrypted"
echo "Publishing"
./gradlew clean publishApkRelease --no-daemon
echo "Deleting keys"
rm -f signing/app-release.jks
rm -f signing/play-account.p12
