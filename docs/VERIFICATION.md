# PoC verification ledger

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
