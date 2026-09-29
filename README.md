# TimsFit

A small, offline Android app for Push / Pull / Legs training. Built with Kotlin and Jetpack Compose. Version 0.2 adds explicit workout choice and simpler screens.

## Try the PoC

Build the APK on this Mac:

```sh
./scripts/gradle.sh :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Android 8.0 or newer is required.

To install on an Android device with USB debugging enabled and this Mac authorized:

```sh
~/.local/share/timsfit/android-sdk/platform-tools/adb devices
python3 scripts/backup-install.py install --serial DEVICE_SERIAL --saved --apk app/build/outputs/apk/debug/app-debug.apk
```

Replace `DEVICE_SERIAL` with the phone's serial from `adb devices`; the emulator has a separate serial. Save edits in the app first: `--saved` acknowledges this before the app is stopped for a consistent snapshot. Every install requires a fresh verified backup on this Mac. The helper saves timestamped archives under `~/Documents/TimsFit Backups`, verifies their contents and checksum, then installs in place. Backup errors stop installation. Keep older backups. Do not bypass this by uninstalling, clearing storage, raw adb installation, or manually installing an APK. A confirmed absent app gets a no-existing-install receipt instead of a data archive.

To use the prepared local foldable emulator:

```sh
./scripts/emulator.sh
python3 scripts/backup-install.py install --serial emulator-5554 --saved --apk app/build/outputs/apk/debug/app-debug.apk
~/.local/share/timsfit/android-sdk/platform-tools/adb -s emulator-5554 shell am start -n com.timsfit.app/.MainActivity
```

## Workout loop

1. Choose **Push**, **Pull**, or **Legs**, then tap **Create workout**. The suggested choice follows completed sessions, but you can override it. An unfinished day opens or resumes instead of creating a duplicate.
2. Review the four-exercise plan, optionally change its split before starting, and tap **Start workout**. Estimated duration is 31–32 minutes including warmup, set execution, rests, and transitions; actual duration depends on your pace.
3. Enter your working weight in **lb** and reps, mark completion, and tap **Save set**. Dumbbell weights are per hand. First-time loads start at zero so you can choose them.
4. Finish after at least one completed set. Partial sessions are labeled with the actual completed set count. Reopen history to edit saved sets.
5. The next same-split session uses previous completed loads. Meeting all three set targets suggests a small increase in positive loads; incomplete sets and zero loads do not earn an increase. You can edit every suggestion.

Completed sessions fill the habit heatmap using the device's local date. All data stays in this app's private storage and survives restarts. Clearing app data or uninstalling removes that local history; cloud sync and export are outside this PoC.

## Development and verification

The first setup installed the toolchain under `~/.local/share/timsfit`, without changing shell settings. `scripts/gradle.sh` selects it automatically. On a fresh Apple Silicon Mac, run `scripts/bootstrap-macos.sh --emulator` and review/accept the Android SDK licenses when prompted.

```sh
./scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
ANDROID_SERIAL=emulator-5554 TIMSFIT_EDITS_SAVED=1 ./scripts/gradle.sh :app:connectedDebugAndroidTest
```

Instrumentation checks run on a dedicated test emulator, explicitly selected above so a connected phone is excluded. The wrapper also takes a verified backup before invoking Gradle; phone instrumentation is blocked. `TIMSFIT_EDITS_SAVED=1` acknowledges that test-device edits are saved before stopping the app. Consult the current test setup before running them against a device containing workouts you want to keep.

`core` contains deterministic generation and state transitions. `app` contains Compose UI, state management, and versioned atomic local persistence. There is no backend or runtime network requirement.

Product scope and chat ownership: [docs/PROJECT.md](docs/PROJECT.md). Shared contract: [docs/CONTRACT.md](docs/CONTRACT.md). Actual verification results and remaining gaps: [docs/VERIFICATION.md](docs/VERIFICATION.md).

## Back up without installing

```sh
python3 scripts/backup-install.py backup --serial DEVICE_SERIAL --saved
```

Backups contain private saved data, including atomic-write sidecars, and stay outside Git. They cover saved entries, not unsaved text in the UI. Do not delete the backup directory during repository cleanup. The current debug app supports this via `run-as`; if a future release cannot be read, installation must stop until another verified backup method is available. Restoring data is a separate deliberate operation: retain the archive/manifest and validate version compatibility before replacing any phone data.
