#!/usr/bin/env bash
# Starts the test foldable. Install image with bootstrap-macos.sh --accept-licenses --emulator.
set -euo pipefail
T_TOOLS="${TIMSFIT_TOOLS:-$HOME/.local/share/timsfit}"
export JAVA_HOME="${JAVA_HOME:-$T_TOOLS/jdk-21.0.12.1+1/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$T_TOOLS/android-sdk}"
T_AVD="${TIMSFIT_AVD:-TimsFit_API36}"
if ! "$ANDROID_HOME/emulator/emulator" -list-avds | grep -Fxq "$T_AVD"; then
  printf 'no\n' | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$T_AVD" -k 'system-images;android-36;google_apis;arm64-v8a' -d '7.6in Foldable'
fi
exec "$ANDROID_HOME/emulator/emulator" -avd "$T_AVD" -no-audio -no-boot-anim -no-snapshot "$@"
