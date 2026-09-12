# Dispatch — Explorer M4-3 (Gen 2)

## 2026-09-06T19:29:45+08:00
You are Explorer M4-3 (Gen 2) for Milestone 4: Settings Screen & Modal Sheets/Dialogs.
Your focus is ViewModel state bindings, Room/Security/Sync preservation, and test invariants.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator_gen2/PROJECT.md`.
- DO NOT modify source code files. You are READ-ONLY.
- Write your findings, architecture blueprint, and code snippets to:
  - Working directory: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_3_gen2`
  - Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_3_gen2/report.md`
  - Handoff: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_3_gen2/handoff.md`
- Send completion message to parent when finished.

## Investigation Scope
1. Inspect `SettingsViewModel.kt`, `AppLockManager.kt`, `PinCipher.kt`, `SyncManager.kt`, and `BackupManager.kt`.
2. Map all user actions in Settings to ViewModel calls:
   - Sync toggle & trigger manual sync.
   - PIN enable/disable/change with `isPickerActive` and biometric authentication.
   - Backup creation, restore from file (safely handling `isPickerActive = true`).
   - Import TXT diary (handling `isPickerActive = true` for document tree picker).
3. Verify test requirements from `tier1_features` and `tier2_boundaries`:
   - Identify existing tests covering Settings and ensure 100% preservation.
4. Formulate integration guidelines and code for Worker M4.
