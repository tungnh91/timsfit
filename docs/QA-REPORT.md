# Independent QA report — 2026-09-25

Production revision tested: fcc3bfb (neutral palette and launcher resources included). Independent test commits: e80e35c and d9c70ce. No production files changed by QA. No release performed.

## Automated evidence

Final command: `scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain`

Result: BUILD SUCCESSFUL in 24s; 14 core tests (4 independently authored), 5 storage tests, 2 instrumentation tests. Existing unit results were up-to-date in final combined run, having passed earlier this session. The 2 instrumentation tests ran again against the final APK. See final-checks.log, independent-instrumentation.log, and Gradle XML reports.

The original instrumentation test failed because performScrollTo targeted a node not yet composed by LazyColumn. Test mechanics were corrected to scroll the container to the node, then green verified. Added isolation before Activity launch and independent invalid-input/recreation/unsaved-warning/save/finish coverage. No red production regression was identified; do not describe the test-mechanics failure as a product defect.

Lint: 0 errors, 9 warnings. Seven concern available dependency versions; two concern redundant v26 resource folder and missing monochrome adaptive-icon layer. See final-lint.txt.

## Direct emulator interaction

- Created and started Push; entered bench 37.5 lb × 8, marked completed, saved. Force-stop/relaunch retained the active workout, weight and completed status.
- Logged lateral raise 10 lb × 12 as completed. Finished partial workout. History showed 2/12 completed; next created workout was Pull.
- Narrow layout at wm size 840x2100 (320 dp at current density) displayed usable inputs; keyboard entry, scrolling and saving worked. Expanded display 1768x2208 inspected. Device-state 1/3 requests did not change this AVD's display dimensions, so this is simulated width/recreation evidence, not true fold-cover/physical-device validation.
- Final APK: cancelled Pull discard, reopened confirmation, confirmed discard. Saved history remained and next split stayed Pull.
- Final APK: opened history, changed bench 37.5 to 40 lb, saved, returned home and reopened. UI and JSON showed 40.0; original completedAt retained; only completed Push remained.
- Final APK: heatmap semantics showed 2026-09-25: 1 completed sessions; summary count1 and history2/12 matched. Screenshot final-heatmap-history.png.
- Invalid weight, draft retention across Activity recreation, unsaved navigation warning/Keep editing, and recreation after save passed instrumentation.
- UI semantics expose exercise, set index, weight unit, reps and completed descriptions. No TalkBack traversal was performed.

## Failure injection / durability

Before final cosmetic/resource changes: made private files directory unwritable; Start displayed Not saved and left draft unstarted. Byte comparison of before-write-failure.json and after-write-failure.json passed. Restored permissions; retry succeeded.

Backed up real UI-generated log, injected qa-invalid-json, force-stopped/relaunched. Protected load error and Retry loading appeared; retry preserved exact corrupt bytes. Restored valid backup and Retry loaded previous workouts. See corruption.png, corrupt-preserved.txt, recovered-hierarchy.txt. This was deliberate test data, not user data.

Atomic storage, serialized IO on Dispatchers.IO, save-before-success, no network permission and core acceptance rules also reviewed statically. No confirmed blocking functional defect found.

## Final artifacts and limits

Final screenshots: final-home-expanded.png, final-home-narrow.png, final-workout-expanded.png, final-workout-narrow.png, final-heatmap-history.png, final-history-edit.png, final-clean-home.png. Earlier screenshots without final prefix predate neutral palette; do not use for final presentation.

Final APK adb am start -W after force-stop: COLD TotalTime982ms (empty log),1068ms (two-workout log). This is limited emulator activity-start timing, not physical-device performance or sustained frame-rate evidence. Raw logs final-cold-launch-1.txt and final-cold-launch-2.txt.

Crash buffer and AndroidRuntime error filter were empty after direct flow (crash-logcat.txt, android-runtime-errors.txt). Expected injected IO failure is covered above. Source manifest has no INTERNET permission.

No physical Galaxy Z Fold tested. No release/deploy. Emulator restored to expanded/open state and app test data cleared; final APK installed and clean Push home left open for user. Test logs and screenshots retained here.
