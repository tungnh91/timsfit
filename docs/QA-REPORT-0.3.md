# Independent QA — TimsFit 0.3

Date: 2026-09-29 America/Los_Angeles. Production revision6c3b693; independent test commit6ba01f8. No product files changed by QA. No blocking defect found in the assessed scope. No phone actions or release performed.

## Integrated checks

Command: `ANDROID_SERIAL=emulator-5554 TIMSFIT_EDITS_SAVED=1 scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain`

PASS, BUILD SUCCESSFUL29s. 22core +13app unit +6instrumentation =41 tests, zero reported failures. Lint0errors10warnings (existing warnings plus UseKtx). Evidence integrated-checks.log and lint.txt. All emulator test output names TimsFit_API36 only. A fresh verified existing-data backup preceded this command.

New independent tests:
- `IndependentWriterTest.lifecycleTimeoutDoesNotAcknowledgeOrLosePendingWrite`: blocked disk exceeds two-second lifecycle barrier; timeout returns without false acknowledgment; queued edit commits after release.
- `IndependentWriterTest.rejectedTransformDoesNotDropLaterEditsOrOrderedFinish`: invalid transform does not poison later edits across exercises or ordered completion.
- `IndependentAutosaveTest.invalidDraftSurvivesRecreationAndFinishKeepsLastValidAutosave`: valid30/7 autosaves, blank invalid reps survives recreation, Finish warns, Continue retains last valid values; no Save/checkbox controls.

Existing writer tests independently read and rerun cover rapid edits with delayed IO, latest-state ordering, disk failure queue retention and Retry including completion. No product bug/red failure found by this reviewer; worker's red-green evidence remains separately in HANDOFF.md.

## Direct runtime / actual upgrade

Created separate authorized AVD TimsFit_QA_Upgrade_API36 at explicit port5556; verified ro.kernel.qemu=1 and boot completed. Never downgraded, uninstalled or cleared app data manually.

Installed packaged0.2 on positively confirmed absent emulator5556 through backup-install.py. Actual0.2 UI created/started Push, logged synthetic30lb, marked completed, explicitly saved and finished. Fresh verified backup then guarded in-place install of final0.3. Pre/post saved-file SHA256 equality passed; old completed workout retained. Evidence old-install.log, upgrade-install.log, pre-upgrade.sha256, upgrade-result.txt. Upgrade backup manifest records old version2; new APK metadata version3/0.3.0. No raw saved JSON exported to repo or printed.

In0.3 chose Push: actual30 carried forward without increment. Started, edited reps to7: autosaved and implicitly completed, with no Save button/checkbox. After Saved acknowledgment, force-stop/relaunch retained30/7/completed and original start timestamp. Elapsed UI advanced across restart. Finished and reopened history; Duration unchanged across two UI snapshots separated by additional elapsed wall time. New Push again prefills30, untouched sets remain uncompleted (automated tests also cover fractional32.5 retention and integer35 formatting).

Inspected expanded1768x2208 and simulated narrow840x2100 (320dp). Text fields, timer, integer labels and Demo action remain usable. These are emulator width/recreation checks, not physical Fold tests.

Screenshots: history-prefill.png, active-expanded.png, restarted-narrow.png, completed-duration.png, finished-home.png, final-home.png.

## Demo evidence and limits

Source maps all12 stable exercise IDs to direct HTTPS publisher pages; rerun unit test covers all IDs. Compared mappings with docs/EXERCISE-LINKS.md provenance; machine-specific lying leg curl / standing calf raise and dumbbell Romanian-deadlift variants are explicit. ACTION_VIEW+BROWSABLE launch with ActivityNotFoundException/SecurityException handling reviewed.

Independently retrieved publisher pages for [barbell bench](https://www.muscleandstrength.com/exercises/barbell-bench-press.html), [incline dumbbell bench](https://www.muscleandstrength.com/exercises/incline-dumbbell-bench-press.html), [dumbbell Romanian deadlift](https://www.dvidshub.net/video/639938/dumbbell-romanian-deadlift), [standing machine calf raise](https://www.muscleandstrength.com/exercises/standing-machine-calf-raise), plus publisher search results for triceps pushdown/seated row/lateral raise. Other direct Muscle & Strength retrievals intermittently returned tool errors; this reviewer does not independently certify all12 live media endpoints.

Direct Demo tap opened Chrome, passed first-run prompts without an account, and displayed the matching bench-press HTTPS URL (demo-browser.png). Actual video playback and full page load were not established; external network/provider remains a limitation. Missing-browser runtime error was verified by implementation worker, not repeated by independent QA.

## Backup receipts — independently verified checksums

All under `/Users/timnguyen/Documents/TimsFit Backups`; existing-data archives and manifest checksums rechecked. Prior backups retained. Install helper reported success only after verification.

- Before integrated tests5554, existing data: `emulator-5554-04ab3fc3/20260930T001237.251063Z-56490b2d`.
- Before original0.2 install on new5556, confirmed absence receipt: `emulator-5556-58878876/20260930T001340.617838Z-90864a87`.
- Before actual0.2→0.3 update5556, existing data archive: `emulator-5556-58878876/20260930T001424.456140Z-527ec26c`.
- Before final0.3 install5554 after normal test-runner cleanup, confirmed absence receipt: `emulator-5554-04ab3fc3/20260930T001658.376945Z-acd036a8`.

All commands explicitly targeted5554 or5556; connected tests used both required environment acknowledgments. Physical phone never targeted. Saved status verified before stopping the app during direct workflow.

## Remaining limits / handoff

Lifecycle onStop can block UI for up to2seconds while IO drains. A slow or failed disk write is not durable until acknowledged; abrupt process death before commit can lose pending text. Unit timeout/failure checks establish honest acknowledgment and queue behavior while process remains alive, not recovery of uncommitted text after disk failure/process death.

Emulator5554: final0.3 installed, clean home open after guarded install. Emulator5556: final0.3, retained synthetic completed history and unstarted Push draft, expanded display, no unsaved edits. New AVD retained for reuse; both slots released. Root-owned README/coordination edits preserved. Root owns packaging and any phone-install handoff.
