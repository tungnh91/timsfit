#!/usr/bin/env bash
# User-local Apple Silicon toolchain. Does not edit shell/system settings.
set -euo pipefail
[[ "$(uname -s)-$(uname -m)" == Darwin-arm64 ]] || { echo 'This bootstrap requires Apple Silicon macOS.' >&2; exit 1; }
T_TOOLS="${TIMSFIT_TOOLS:-$HOME/.local/share/timsfit}"
T_DOWNLOADS="$T_TOOLS/downloads"
export JAVA_HOME="$T_TOOLS/jdk-21.0.12.1+1/Contents/Home"
export ANDROID_HOME="$T_TOOLS/android-sdk"
mkdir -p "$T_DOWNLOADS" "$ANDROID_HOME/cmdline-tools"
fetch_verified() {
  local url="$1" destination="$2" checksum="$3"
  if [[ ! -f "$destination" ]]; then curl -fL --retry 3 "$url" -o "$destination"; fi
  echo "$checksum  $destination" | shasum -a 256 -c -
}
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  fetch_verified 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.12.1_1.tar.gz' "$T_DOWNLOADS/jdk21.tar.gz" 3623232f33a9c3baadf304480b2535f9a3cba8a58d42ecbb438ba267315d9998
  tar -xzf "$T_DOWNLOADS/jdk21.tar.gz" -C "$T_TOOLS"
fi
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  fetch_verified 'https://dl.google.com/android/repository/commandlinetools-mac_arm64-15859902_latest.zip' "$T_DOWNLOADS/cmdtools.zip" 835b62a26162b229b441d1f6d4680383815a270809eb33522c0d480fa5002c4e
  unzip -q "$T_DOWNLOADS/cmdtools.zip" -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
T_SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
# Accept SDK licenses interactively; --accept-licenses is for an authorized setup.
if [[ "${1:-}" == --accept-licenses ]]; then
  set +o pipefail
  yes | "$T_SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses
  T_STATUS=${PIPESTATUS[1]}
  set -o pipefail
  [[ "$T_STATUS" == 0 ]]
else
  "$T_SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses
fi
"$T_SDKMANAGER" --sdk_root="$ANDROID_HOME" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
if [[ " ${*} " == *' --emulator '* ]]; then
  "$T_SDKMANAGER" --sdk_root="$ANDROID_HOME" 'emulator' 'system-images;android-36;google_apis;arm64-v8a'
fi
"$(dirname "$0")/gradle.sh" --version
