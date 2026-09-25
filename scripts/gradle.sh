#!/usr/bin/env bash
set -euo pipefail
T_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
T_TOOLS="${TIMSFIT_TOOLS:-$HOME/.local/share/timsfit}"
if [[ -z "${JAVA_HOME:-}" ]]; then
  for T_JDK in "$T_TOOLS"/jdk-*/Contents/Home; do
    if [[ -x "$T_JDK/bin/java" ]]; then export JAVA_HOME="$T_JDK"; break; fi
  done
fi
export ANDROID_HOME="${ANDROID_HOME:-$T_TOOLS/android-sdk}"
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$ANDROID_HOME/platform-tools:$PATH"
cd "$T_ROOT"
exec ./gradlew "$@"
