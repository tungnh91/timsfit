#!/usr/bin/env bash
set -euo pipefail
T_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
T_NEEDS_BACKUP=false
for T_ARG in "$@"; do
  # Gradle accepts task abbreviations; require complete known task names so
  # shortcuts such as cDAT/iD cannot bypass the pre-install backup.
  if [[ "$T_ARG" != -* ]]; then
    T_TASK="${T_ARG##*:}"
    case "$T_TASK" in
      help|tasks|projects|properties|dependencies|dependencyInsight|clean|build|check|test|testDebugUnitTest|testReleaseUnitTest|assemble|assembleDebug|assembleRelease|assembleDebugAndroidTest|compileDebugKotlin|compileReleaseKotlin|compileKotlin|lint|lintDebug|lintRelease|installDebug|installRelease|connectedDebugAndroidTest|connectedCheck) ;;
      *) echo 'Use a supported full Gradle task name; abbreviated/unknown tasks are blocked to protect device backups.' >&2; exit 2 ;;
    esac
  fi
  if [[ "$T_ARG" == *connected*AndroidTest* || "$T_ARG" == *install* || "$T_ARG" == *Install* || "$T_ARG" == *connectedCheck* ]]; then
    T_NEEDS_BACKUP=true
    if [[ -z "${ANDROID_SERIAL:-}" || "${TIMSFIT_EDITS_SAVED:-}" != 1 ]]; then
      echo 'Install/device tests require explicit ANDROID_SERIAL and TIMSFIT_EDITS_SAVED=1 after saving workout edits. A verified Mac backup is mandatory.' >&2
      exit 2
    fi
    if [[ "$T_ARG" == *connected* && "$ANDROID_SERIAL" != emulator-* ]]; then
      echo 'Destructive instrumentation is restricted to dedicated emulators. Use backup-install.py for phone updates.' >&2
      exit 2
    fi
  fi
done
if [[ "$T_NEEDS_BACKUP" == true ]]; then
  python3 "$T_ROOT/scripts/backup-install.py" backup --serial "$ANDROID_SERIAL" --saved
fi
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
