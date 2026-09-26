#!/usr/bin/env bash
set -euo pipefail
for T_ARG in "$@"; do
  if [[ "$T_ARG" == *connected*AndroidTest* && -z "${ANDROID_SERIAL:-}" ]]; then
    echo 'Set ANDROID_SERIAL explicitly before running device tests (for example emulator-5554). Tests replace app test data.' >&2
    exit 2
  fi
done
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
