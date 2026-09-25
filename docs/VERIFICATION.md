# PoC verification ledger

Owner: Chief of Staff. All checks below are pending until evidence is recorded.

## Required acceptance checks

| Area | Observable check | Status |
|---|---|---|
| TDD | Core and storage tests fail for missing behavior, then pass after implementation; retain command evidence | Pending |
| Generation | First Push, completed Push → Pull → Legs → Push; drafts/discard do not advance | Pending |
| History | Same exercise uses latest same-split completed history; increase only after target completion; partial performance retains load | Pending |
| Time budget | Generated plans estimate 30–40 minutes including rest/warmup, standard gym exercises | Pending |
| Logging | Valid set edit/completion saved; invalid reps/NaN/infinite/negative weight rejected clearly | Pending |
| Lifecycle | Active session and logged values survive process death/relaunch; activity recreation preserves screen/input | Pending |
| Completion | Cannot finish empty/unstarted workout; partial completion allowed; double finish does not duplicate history | Pending |
| Persistence | Atomic versioned storage roundtrip; missing file empty state; corrupted file preserved with recovery/error; write failures shown | Pending |
| Habit | Completion-only heatmap, multiple sessions/day, local midnight/timezone/DST correctness | Pending |
| UI | Start/log/edit/finish/reopen/next-day end-to-end, empty state, discard confirmation, history editing | Pending |
| Layout | Narrow phone and expanded layout, scrolling/keyboard accessibility, meaningful labels/tap targets | Pending |
| Offline/speed | No runtime network needed, responsive local interactions, IO off main thread | Pending |
| Build | Integrated core/app tests, lint, debug APK, instrumentation tests | Pending |
| Independent review | Separate chat evaluates intended behavior and flags/fixes before handoff | Pending |
| Physical device | Galaxy Z Fold 8 interaction/performance on actual hardware | Pending device availability |

## Evidence

No app checks have run yet. Do not infer verification from compilation or isolated worker claims.
