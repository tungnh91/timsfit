# TimsFit delivery record

Updated: 2026-09-29. Owner: Chief of Staff chat `01a0da95-718e-79a1-b33b-2464dd079252`.

## Current revision — 0.3 logging, timer and demos (2026-09-29)

User requested a new implementation chat for: prefilled actual exercise history values; integer formatting without trailing `.0`; automatic saving; valid edited sets count as completed without a checkbox; elapsed session timer from Start; verified external exercise demonstration links. Maintain minimal copy, PPL overrides, existing history, and mandatory verified Mac backup before any install. Implementation is dispatched to a new isolated worktree at baseline `e2c9fae`; Chief of Staff retains integration and independent QA. Implemented and integrated as `6c3b693`; independent tests `6ba01f8`. Integrated checks pass: 22 core + 13 app unit + 6 instrumentation = 41 tests, lint and APK build. Emulator 0.2→0.3 upgrade preserved saved JSON byte-for-byte. Independent source review and emulator acceptance found no blocking defect; the detailed report is linked in the verification ledger. User authorized phone update on 2026-09-29: verified local backup, in-place 0.2→0.3 installation and launch succeeded on RFGL71KQHAB. Saved workout file compared byte-identical before launch. No store release. Selected interpretation: actual previous values prefill directly (30 → 30); untouched suggestions are not completed; valid edits persist automatically and complete that set.

Implementation chat `01a0ef98-895c-7521-a398-1ef7fca317be`, “Improve workout logging, timer, and exercise demos”, active in `/Users/timnguyen/.codex/worktrees/00cd/TimsFit` on `codex/autosave-history-timer`. Implementation complete; emulator handed to independent QA with mandatory verified backup before installs/tests.

## Previous revision — 0.2

User feedback: simplify text; remove motivational copy; allow Push/Pull/Legs selection overriding recommendation. Scope/API/acceptance are in `docs/CONTRACT.md` revision0.2. Implemented and integrated: concise copy, manual split choice, safe draft switching, and supplied biceps icon. 29 tests and independent emulator QA pass; original0.1→0.2 upgrade retained saved JSON byte-for-byte. Main owns records. Installation chat verified final0.2 installed and launched on authorized Samsung phone (versionCode2/versionName0.2.0). No physical workout acceptance test was run.

## Original project state

Discovery found an empty project directory, no existing code, instructions, tests, or Git history. Initialized Git on `main`; this is the integration branch. The first native PPL app is now implemented and integrated on main. All 21 automated tests, lint, build, and independent emulator acceptance checks pass. No release has been published.

## Product brief

TimsFit is a new fitness app. User context from “Android App Development Loop” (`6ab6eda6-2558-83ea-9193-ee67dc9da086`): own Android fitness tracker for Galaxy Z Fold 8; prefers native and fast. Kotlin/Compose was recommended in that chat, not explicitly selected by the user. The user subsequently explicitly selected Kotlin + Jetpack Compose with local persistence.

“PPL Workout Plan” (`6aac8d79-952c-83e9-834b-55bbe0c465cc`) records a request for a three-day PPL plan. “Fitbod alternatives apps” (`6aac8d72-1ee8-83ea-9b12-fb431a379db0`) records interest in alternatives. Neither establishes a TimsFit requirement for workout generation or coaching. “Write chief of staff prompt” (`01a0da93-b9c2-7661-8506-8bc5b21e1038`) supplies coordination process, no product specification.

## Agreed first milestone — native PPL PoC

Personal offline strength-training app: create the next PPL day from completion history, get a 30–40 minute plan, log/edit sets, finish, reopen history, and track completed days on a habit heatmap. No account, cloud sync, AI coaching, health integrations, subscriptions, or public release in this milestone.

Agreed observable acceptance criteria:
1. Create the next PPL workout from completed history; plans use standard gym equipment and estimate 30–40 minutes. Start and log multiple exercises/sets offline.
2. Edit a set; reject invalid weights/reps with understandable feedback. Units remain explicit and consistent.
3. Restart the process during a session; saved entries and the active workout survive.
4. Finish a workout once; history reopens its correct exercises, sets, and local date/time.
5. Narrow and expanded layouts remain usable; resizing/recreation preserves the workout. Inputs have accessible labels.
6. Completed days appear correctly on a habit heatmap. Red–green tests, build and focused automated checks pass; independent review and direct app interaction verify the combined result. Record unavailable device checks as gaps.

User confirmed Kotlin + Jetpack Compose, generated Push/Pull/Legs workouts based on history, 30–40 minutes, standard gym equipment, a simple habit graph/heatmap, and a light minimal design inspired by Airbnb/Apple. Red–green TDD and separate implementation/testing chats are required. See `docs/CONTRACT.md` for the implementation contract.

## Milestone completion and next backlog

1. Completed: native PPL generation, logging, persistence, history and heatmap integrated.
2. Completed: red–green TDD, independent unit/UI tests, direct emulator durability/error/layout acceptance, final APK packaging.
3. Next: user tries APK on physical Galaxy Fold; collect actual device feedback.
4. Completed from feedback: concise screens and manual workout selection (revision0.2), plus supplied icon.

## Decisions and assumptions

- Standing user instruction, 2026-09-29: before EVERY install/update, make and verify a fresh local backup to `~/Documents/TimsFit Backups`. No install after failed/unreadable backup; positively confirmed absent app requires a no-existing-install receipt. Keep prior backups, never uninstall/clear data as a workaround. Applies to all project chats and test-device installs. Implemented `scripts/backup-install.py` for backup-only or backup-then-install; README documents commands. Gradle wrapper also backs up before supported install/test tasks, blocks abbreviations and phone instrumentation. 25 fake-device host tests passed, including 13 independently authored checks. No live backup/install was performed for this change; saved-edit acknowledgment is required and no automated restore exists. See AGENTS.md.

- Only the Chief of Staff edits coordination records and integrates changes to `main`.
- Modest concurrency; bounded real project chats, reused when relevant.
- Kotlin + Jetpack Compose explicitly selected. Deterministic local generation; no remote AI or backend. PoC uses explicitly labeled pounds.
- Bootstrap owner may install user-local native Android prerequisites and emulator for build/testing. No global configuration changes.
- No background monitoring or scheduled continuation has been configured.
- Device tests now require explicit ANDROID_SERIAL through scripts/gradle.sh. A cancelled unscoped QA run may have affected the connected phone installation; cause unresolved. See docs/QA-REPORT-0.2.md. Do not claim phone data was preserved.

## Workstream registry

| Chat | Assignment | Checkout / branch | Dependency | Status |
|---|---|---|---|---|
| `01a0da95-718e-79a1-b33b-2464dd079252` | Product decisions, records, coordination, integration | `/Users/timnguyen/dev/TimsFit`, `main` | None | Active |
| `01a0da96-e152-7192-8d96-1a21ed890ebc` | Android toolchain and project bootstrap | Main; commits `ef002c8`, `45d3084`, `81d3905` | None | Integrated; packaging and emulator boot verified; no product tests |
| `01a0da9d-2677-7fa3-8956-89c57779267c` | PPL engine, models and red–green unit tests | `/Users/timnguyen/.codex/worktrees/b143/TimsFit`, `codex/core-engine` | None | Integrated; 10 original tests passed on main; 4 independent added |
| `01a0da9d-2678-7f30-b350-af1b97355bd4` | Compose UI, durable storage, storage/UI tests | `/Users/timnguyen/.codex/worktrees/6d9f/TimsFit`, isolated checkout | None | Integrated; unit and independent runtime verification passed |
| `01a0daa0-983b-7953-8eff-b231922ef03c` | Independent acceptance testing/review | Main; initially read-only | None | Complete; no confirmed blocking defect |
| `01a0daae-f07a-7633-bf55-f1917a3059f8` | User-requested step-by-step phone installation and testing guidance | Main; guidance/device setup only | Phone connection | Dispatched; user interacts directly in this chat |

## Verification and unresolved issues

- Inspected directory and relevant chats: complete.
- Prior PoC verified with 21 automated tests and direct emulator interaction; revision0.2 and revision0.3 verification complete (see ledger).
- Native toolchain installed user-locally under `~/.local/share/timsfit`. `scripts/gradle.sh` selects JDK/SDK. Bootstrap packaging passes (NO-SOURCE is not a behavioral test pass).
- API 36 ARM64 foldable emulator booted as `emulator-5554`; no physical Galaxy connected. Bootstrap specialist verified boot complete and folded/open states available. Product interaction completed; see `docs/VERIFICATION.md` and `docs/QA-REPORT.md`.
- User workflow decision resolved. PPL PoC is agreed; implemented, independently verified, integrated and packaged. Latest APK `artifacts/TimsFit-0.3.0-debug.apk`; local PoC, not store released.
