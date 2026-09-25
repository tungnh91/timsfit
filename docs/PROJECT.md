# TimsFit delivery record

Updated: 2026-09-25. Owner: Chief of Staff chat `01a0da95-718e-79a1-b33b-2464dd079252`.

## Current state

Discovery found an empty project directory, no existing code, instructions, tests, or Git history. Initialized Git on `main`; this is the integration branch. The first native PPL app is now implemented and integrated on main. Domain and storage unit tests pass; independent emulator/lint verification is underway. No release has been published.

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

## Prioritized backlog

1. Workflow and stack confirmed; contract established.
2. Audit Android SDK/JDK/emulator/device availability and identify a reproducible build path.
3. Finalize minimal data/UI contracts and assign isolated implementation after scope is settled.
4. Implement the agreed vertical slice; independently test, fix, and integrate it.
5. Provide APK/runnable preview and verified handoff, with any physical-device gaps explicit.

## Decisions and assumptions

- Only the Chief of Staff edits coordination records and integrates changes to `main`.
- Modest concurrency; bounded real project chats, reused when relevant.
- Kotlin + Jetpack Compose explicitly selected. Deterministic local generation; no remote AI or backend. PoC uses explicitly labeled pounds.
- Bootstrap owner may install user-local native Android prerequisites and emulator for build/testing. No global configuration changes.
- No background monitoring or scheduled continuation has been configured.

## Workstream registry

| Chat | Assignment | Checkout / branch | Dependency | Status |
|---|---|---|---|---|
| `01a0da95-718e-79a1-b33b-2464dd079252` | Product decisions, records, coordination, integration | `/Users/timnguyen/dev/TimsFit`, `main` | None | Active |
| `01a0da96-e152-7192-8d96-1a21ed890ebc` | Android toolchain and project bootstrap | Main; commits `ef002c8`, `45d3084`, `81d3905` | None | Integrated; packaging and emulator boot verified; no product tests |
| `01a0da9d-2677-7fa3-8956-89c57779267c` | PPL engine, models and red–green unit tests | `/Users/timnguyen/.codex/worktrees/b143/TimsFit`, `codex/core-engine` | None | Integrated; 10 original tests passed on main; 4 independent added |
| `01a0da9d-2678-7f30-b350-af1b97355bd4` | Compose UI, durable storage, storage/UI tests | `/Users/timnguyen/.codex/worktrees/6d9f/TimsFit`, isolated checkout | None | Integrated; storage green; runtime verification underway |
| `01a0daa0-983b-7953-8eff-b231922ef03c` | Independent acceptance testing/review | Main; initially read-only | None | Independent unit/runtime/lint verification active |

## Verification and unresolved issues

- Inspected directory and relevant chats: complete.
- Existing app tests: none; no app exists.
- Native toolchain installed user-locally under `~/.local/share/timsfit`. `scripts/gradle.sh` selects JDK/SDK. Bootstrap packaging passes (NO-SOURCE is not a behavioral test pass).
- API 36 ARM64 foldable emulator booted as `emulator-5554`; no physical Galaxy connected. Bootstrap specialist verified boot complete and folded/open states available. Product interaction pending integration.
- User workflow decision resolved. PPL PoC is agreed; implementation and verification underway.
