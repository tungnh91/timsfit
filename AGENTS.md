# TimsFit coordination

The user interacts with the Chief of Staff chat: `01a0da95-718e-79a1-b33b-2464dd079252`.
The user explicitly authorizes project specialist chats to communicate with that chat, and authorizes it to create chats, assign work, inspect progress, and integrate results.

Read `docs/PROJECT.md` before work. The Chief of Staff owns that file and shared coordination records. Specialists must follow their assignment's ownership boundaries, report blockers and results to the Chief of Staff, and must not expand scope or create additional chats without direction. Do not ask the user to relay messages.

Preserve user changes. Use separate worktrees for concurrent implementation. Report implemented, tested, integrated, and released status separately; never report an unrun check as passing. Significant features require independent review against acceptance criteria and integrated verification.

## Mandatory backup before installing

User instruction (2026-09-29): always back up existing TimsFit data to this computer before any install/update. This applies to every project chat and installation route, including test setups that install the app. Select the device explicitly. Save a fresh, timestamped backup outside the Git repository under `~/Documents/TimsFit Backups`, verify the archive and checksum, and retain prior backups. Include saved files and any atomic-write sidecars; do not print workout contents into chat/logs.

Do not install if backup fails or existing data cannot be read. Only a positively confirmed absent app may proceed with a recorded no-existing-install receipt. Never treat permission/connection errors as an empty app. Preserve app identity/signing key, use an in-place update, and never uninstall or clear data as a workaround. Backups cover saved data; ensure edits are saved before stopping the app. Report backup location and verification alongside each installation result. User instructions are standing authorization for these local pre-install backups; no repeat permission request is needed.
