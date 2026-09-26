# Independent QA — TimsFit 0.2

Functional production revision: 0e509e4. Independent test commit: 9426c1b. No product changes by QA.

## Verified

Scoped command: `ANDROID_SERIAL=emulator-5554 scripts/gradle.sh :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain` passed. 20 core tests, 5 storage tests, 4 emulator instrumentation tests. Lint 0 errors/9 warnings. See integrated-checks.log. Core/storage ran successfully during preceding build; scoped combined run reused those up-to-date results and ran all four emulator tests.

Independent additions cover repeated split switching without compounding increments, fresh edited same-split history, zero/varied load preservation, chosen Legs completion → suggested Push, reopening chosen history and choosing Legs again retaining saved weight with completion flags reset.

Direct upgrade: installed packaged original artifacts/TimsFit-0.1.0-debug.apk on emulator. Actual old UI created/started Push, saved72.5lb bench ×8 completed, finished, then created Pull draft. `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk` installed0.2. Before/after JSON byte-identical (v01-before-upgrade.json/v02-after-upgrade.json). Package reports show versionCode1/name0.1.0 then versionCode2/name0.2.0. Upgraded old history reopened visibly72.5lb with its original completion timestamp.

Switched old unstarted Pull draft to Legs via selector. ID and createdAt retained; original completed Push unchanged. Force-stop/relaunch retained selected Legs draft. Started Legs, entered95lb squat ×8, marked completed, saved. Second force-stop/relaunch via Resume retained active Legs,95lb and completion flag. Active selector absent. Finished chosen Legs; home suggested Push and heatmap displayed2 completed sessions for today. Completed history has no split selector. See switched-draft.json, active-restarted.json, hierarchy captures.

Visual narrow840x2100 (320dp) and expanded1768x2208: home begins workout card; no slogan/hero/standard gym boilerplate; accessible Push/Pull/Legs controls, units/reps/rest retained; preview sets condensed; default repeated notes removed; per-hand labels visible. Screenshots home-{narrow,expanded}, chosen-legs-draft-{narrow,expanded}, chosen-active-{narrow,expanded}, upgraded-old-history.png. Tested screenshot dimensions are simulated emulator sizes, not physical Fold coverage.

Nonblocking inherited observation: narrow heatmap begins at oldest week; today may require horizontal scroll, though count/date data are correct. No new blocking functional defect found. No unchanged storage failure reinjection; 0.1 durability evidence remains in artifacts/qa.

## Device boundary

Physical phone became connected during initial unscoped build. Cancelled exact Gradle client with SIGINT (exit130). Client output ended at lint, but deeper daemon audit proves connectedDebugAndroidTest had entered execution before cancellation (17:25:42–17:25:53 local build window, cancellation reports two gRPC test-executor errors). Earlier statements that cancellation preceded device-task execution were incorrect and corrected to coordinator.

No direct QA adb command targeted the phone: all used -s emulator-5554. Subsequent scoped UTP log confirms installs and uninstalls only on emulator-5554. Available daemon/current result logs contain no phone serial or explicit phone installation/removal. However, cancelled-run result outputs were replaced by subsequent run; these logs cannot exclude phone setup/cleanup during cancelled unscoped task. Installation helper reported phone package unexpectedly absent. Cause is unresolved; possible test setup impact must be disclosed, and phone-data preservation cannot be claimed from this audit. No phone tools were used to investigate. Evidence: cancellation-daemon-excerpt.txt, device-target-audit.txt, scoped-utp.log.

## Final icon verification

Final product revision1ff18d3 adds supplied biceps adaptive icon. `ANDROID_SERIAL=emulator-5554 scripts/gradle.sh :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest --console=plain` passed in20s, all4 emulator tests rerun against final resources. Core20/storage5 remain earlier passing unchanged checks. Final lint0errors9warnings; see final-lint.txt for exact warning list.

Installed final APK on emulator and visually inspected launcher-icon.png: recognizable biceps with clearance inside circular mask. Tapped launcher TimsFit and confirmed clean home opens (final-home.png/final-home.txt). Earlier home/workout captures share unchanged UI; icon-only final build does not alter those screens. Emulator released in expanded state with final0.2 installed and clean home open after test APK cleanup/reinstall. Root-owned host guard now rejects unscoped connected-device testing. No physical-device QA claim; no release performed.
