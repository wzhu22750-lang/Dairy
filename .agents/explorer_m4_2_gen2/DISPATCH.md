# Dispatch — Explorer M4-2 (Gen 2)

## 2026-09-06T19:29:45+08:00
You are Explorer M4-2 (Gen 2) for Milestone 4: Settings Screen & Modal Sheets/Dialogs.
Your focus is `IosModalDialog.kt`, `IosActionSheet.kt`, and replacing all Android legacy dialogs in Settings.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator_gen2/PROJECT.md`.
- DO NOT modify source code files. You are READ-ONLY.
- Write your findings, architecture blueprint, and code snippets to:
  - Working directory: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_2_gen2`
  - Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_2_gen2/report.md`
  - Handoff: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_2_gen2/handoff.md`
- Send completion message to parent when finished.

## Investigation Scope
1. Inspect `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosModalDialog.kt` and `IosActionSheet.kt`.
2. Inspect all dialog usage in `SettingsScreen.kt` (Theme selection, Font selection, PIN set/change dialog, export dialog, clear data confirmation).
3. Formulate the blueprint for replacing all Android `AlertDialog` / `BasicAlertDialog` with iOS primitives:
   - `IosModalDialog`: 270dp fixed width, 14dp squircle corners, 17sp bold title, 13sp body message, 0.5dp hairline dividers between action buttons, destructive action support (red text).
   - `IosActionSheet`: bottom sheet with rounded action rows and detached Cancel pill for theme/font selection.
4. Formulate drop-in composable code for Worker M4.
