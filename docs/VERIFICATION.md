# PoC verification ledger

## Revision 0.3 — history prefills, autosave, timer and demos

Independent report: [QA-REPORT-0.3.md](QA-REPORT-0.3.md). Production revision `6c3b693`; independent tests `6ba01f8`. VersionCode3/versionName0.3.0, schema1 and package/signing identity unchanged. Implemented and integrated on main; local debug APK packaged, no phone install or store release.

- Actual red–green evidence: new history tests failed before implementation; numeric/timer/writer tests failed before implementation; old Save controls failed the new UI expectation. Logs in `artifacts/qa-v03/`.
- Integrated checks: 22 core + 13 app unit + 6 emulator instrumentation = **41 tests**, zero failures/errors/skips; CoS independently read XML. Lint zero errors/10 warnings; build passed. Independent tests cover invalid draft/recreation/Finish, IO exceeding lifecycle wait, and rejected transforms preserving subsequent operations.
- Runtime checks: automatic 30 lb save survives restart, timer resumes from original start and freezes at finish, next Push prefills actual30 without marking it completed. CoS inspected narrow/expanded screens. Browser launch and missing-browser error exercised; video playback not tested. Demonstration sources in `docs/EXERCISE-LINKS.md`.
- Genuine upgrade on separate emulator-5556: 0.2 saved synthetic workout retained byte-for-byte through guarded in-place 0.3 installation. First-install receipt `20260930T001340.617838Z-90864a87`; pre-upgrade backup `20260930T001424.456140Z-527ec26c` under `~/Documents/TimsFit Backups/emulator-5556-58878876/`. Integrated instrumentation backup `20260930T001237.251063Z-56490b2d` under the emulator-5554 backup directory. No phone operations.
- APK: `artifacts/TimsFit-0.3.0-debug.apk`, 11,686,459 bytes, SHA256 `a10d55196bb31f6fee7699229c2954250d31923516061fc365607ed0590f366e`.
- Limits: no physical-device workout acceptance; no full accessibility/performance audit. Unsaved/failed writes are never acknowledged as Saved; abrupt termination before disk acknowledgment cannot guarantee the pending edit.

## 2026-09-29 — Android absent-package compatibility fix

Worker stopped safely after emulator test cleanup because Android pm path returns empty exit1 for an absent package. Added failing regression, then narrowly accepted empty exit0/1 only after valid absent inventory and rechecked device connectivity plus complete inventory. Output/errors, other exit codes and contradictory inventory still block installs. Full25host tests passed; independent read-only review passed. No device actions performed by CoS for this fix.

## 2026-09-29 — mandatory pre-install backups

`python3 -m unittest discover -s scripts/tests -v`: 22 host tests passed (9 implementation + 13 independent), using fake adb/Gradle only. Independent review found abbreviated Gradle tasks could bypass the initial wrapper; fixed with supported full-task allowlist and verified regressions. Failure cases include device/permission/inventory/disk/archive errors, conflicting absence checks, wrong Android user, unsafe archive paths, sidecars/raw corrupt bytes, saved-edit acknowledgment, explicit serial, phone-instrumentation rejection, and backup-before-install ordering. `bash -n scripts/gradle.sh` and diff checks passed.

No live backup, phone install or restore was performed during this change. The current debug app stores data under credential-encrypted private files, covered by the archive; nondebug access failures block updates, and future external/device-protected storage would require backup coverage changes. Backups remain local outside Git. Automatic restoration is intentionally not implemented.


## Revision 0.2 — simplified screens and chosen workouts

Behavior revision `0e509e4`; independent test additions `9426c1b`; final supplied-icon resource revision `1ff18d3`. VersionCode 2 / versionName 0.2.0.

- Actual core red: 19 tests ran, four failed against missing override/draft behavior; green: 19 passed. Independent repeated-switch/history-edit test brings final core total to 20.
- Actual Compose red: choosing Legs failed because selector was absent. Green: three implementation instrumentation tests; independent full override/history/rotation flow brings final emulator total to four.
- Integrated command: `ANDROID_SERIAL=emulator-5554 scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain`. BUILD SUCCESSFUL in 21 s; 20 core + 5 storage + 4 instrumentation = 29 tests, zero failures/errors/skips. Root independently read XML results. Lint: zero errors, nine pre-existing warnings.
- Upgrade check: original packaged 0.1 app created a completed Push with 72.5 lb bench set and a Pull draft through UI. Installing 0.2 with `adb -s emulator-5554 install -r` retained JSON byte-for-byte and reopened existing history/draft. Draft changed to Legs successfully. No schema change.
- Root inspected simplified expanded screenshots: no slogan, motivational hero, or repetitive default guidance; accessible three-way split choice and functional information retained. Final narrow/expanded screens inspected; chosen draft and 95 lb active Legs set survived force-stop, old history remained unchanged, completed Legs suggested Push.
- Emulator tests were explicitly restricted to emulator-5554 after a physical phone connected. An earlier unscoped command was cancelled during connectedDebugAndroidTest (17:25:42–17:25:53 local). The initial client log hid task startup; a later daemon audit corrected that assessment. Available logs cannot exclude setup/cleanup impact on the newly connected phone, whose 0.1 package subsequently appeared absent. Cause is unresolved; no phone data-preservation claim is made. Subsequent scoped runs explicitly target the AVD only. `scripts/gradle.sh` now rejects connected tests without an explicit ANDROID_SERIAL (exit2 verified). The phone installation chat separately installed 0.1 on the user's authorized phone; revision 0.2 physical acceptance is not claimed.
- Packaged `artifacts/TimsFit-0.2.0-debug.apk`: 11,741,373 bytes, SHA256 `bca20b9b0d40194c0bd72340f3cd5b33ff29bd634ce155d87b66217aeb300d05`. APK metadata independently confirms version 2 / 0.2.0. Final icon-only resource build passed assemble/lint plus all four emulator tests in 20 s; launcher icon visually checked. Logs: `artifacts/qa-v02/`; independent report: `docs/QA-REPORT-0.2.md`.

Final phone handoff: installation chat verified final APK hash, `install -r` succeeded, package version2/0.2.0 confirmed, MainActivity launched successfully (single observed cold launch468 ms). No clear/uninstall/test-seeding by installation chat. Prior phone package was absent; no phone-history preservation or physical workout acceptance claim.

## Original 0.1 verification

Owner: Chief of Staff. Verified native PoC build: `fcc3bfb` (all production code; subsequent documentation-only commits do not change the APK).

## Required acceptance checks

| Area | Observable check | Status |
|---|---|---|
| TDD | Actual core red: 9/9 then 10/10 NotImplementedError; storage red: 5/5 TODO failures. Implementations subsequently green. | Passed |
| Generation | Public-transition full PPL cycle, draft reuse/discard, latest completion ordering | Passed: 14 core tests |
| History | Latest same-split history, varied per-set positive load progression, zero/partial safeguards | Passed: core + direct history edit 37.5 → 40 lb, saved/reopened with completion timestamp preserved |
| Time budget | Computed Push/Pull 31 min, Legs 32 min; four gym exercises × three sets incl warmup/rest/transitions | Passed: arithmetic tests and source review; estimate, not real workout timing |
| Logging | Valid set saved; range, finite and ID/index errors covered; negative input error seen on emulator | Passed |
| Lifecycle | Saved 37.5 lb set survived force-stop/relaunch; 42.5 draft retained across Activity recreation | Passed: direct emulator + instrumentation |
| Completion | Unstarted/empty rejection, partial completion, idempotent finish, next Pull | Passed: core + emulator |
| Persistence | Five store unit tests; unwritable directory error preserved original bytes; malformed JSON preserved on retry and valid-file recovery succeeded | Passed: units + injected emulator failures |
| Habit | Completion-only local dates, midnight/DST/multiple sessions tests | Passed: core + direct heatmap count1/cell and history2/12 match |
| UI | Two end-to-end emulator tests green; create/start/save/finish/history/next Pull/unsaved warnings/empty/error | Passed: discard cancel/confirm retained history and next split; history edit persisted |
| Layout | Expanded 1768×2208 and narrow 840×2100 (~320dp) inspected; labels exercised through Compose semantics; palette corrected after screenshot review | Passed basic visual/semantics checks; full TalkBack audit not run |
| Offline/speed | No INTERNET permission; local generation and Dispatchers.IO saves. Final debug cold starts 982 ms empty / 1068 ms restored on emulator | Passed limited emulator checks; physical/release profiling not run |
| Build | 14 core + 5 storage + 2 instrumented tests; lint and assemble; BUILD SUCCESSFUL in 24 s | Passed |
| Independent review | QA chat 01a0daa0-983b-7953-8eff-b231922ef03c reviewed source, added public-flow tests, exercised actual app | Passed; no confirmed blocking functional defect |
| Physical device | Galaxy Z Fold 8 interaction/performance on actual hardware | Not run: no phone connected. AVD fold-state command did not change display; narrow coverage used wm size, not real cover-display switching. |

## Evidence

Final command: `scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest`.

Root independently inspected test XML: 21 tests, zero failures/errors/skips. Root visually inspected final narrow workout and expanded home screenshots. Independent final report: `docs/QA-REPORT.md`. Final build log: `artifacts/qa/final-checks.log`; screenshots and failure injection evidence: `artifacts/qa/` (ignored generated artifacts).

Lint: zero errors, nine nonblocking warnings (seven newer dependency versions, redundant v26 icon qualifier at minSdk26, monochrome icon lint warning despite v33 variant). Dependencies intentionally remain pinned to the build-tested compatible set.

Deliverable: `artifacts/TimsFit-0.1.0-debug.apk`, 11,640,931 bytes; SHA256 `bf32433d58c5ca48080a483a3598faf29358d57825709c453f329c43504efa2a`. Debug signed PoC, not a store release. Data is local-only, weights in pounds. No cloud sync/export or account exists.
