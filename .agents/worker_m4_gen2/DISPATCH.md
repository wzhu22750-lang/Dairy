# Dispatch — Worker M4 (Gen 2)

## 2026-09-06T19:34:30+08:00
You are Worker M4 (Gen 2) implementing Milestone 4: Settings Screen & Modal Sheets/Dialogs.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator_gen2/PROJECT.md`.
- DO NOT modify non-UI business logic: Room DAOs/entities, Security (`AppLockManager`, `PinCipher`), and Supabase sync must remain 100% untouched.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Write Ownership
You exclusively own and may edit the following files:
1. `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`
2. `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosModalDialog.kt`
3. `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosActionSheet.kt`

## Inputs & Blueprints from Explorers
Read and integrate the blueprints from the M4 Explorers:
- Explorer M4-1: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_1_gen2/report.md`
- Explorer M4-2: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_2_gen2/report.md`
- Explorer M4-3: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m4_3_gen2/report.md`

## Required Implementations:
1. **`IosModalDialog.kt`**:
   - Ensure 270dp fixed width, 14dp squircle shape, 17sp bold title, 13sp body message, 0.5dp hairline dividers between action buttons.
   - Support adaptive button layout (horizontal for 1-2 buttons, vertical column for >=3 buttons).
   - Support destructive action style (`Color(0xFFFF3B30)`).
   - Provide `IosDialogTextField` for text/PIN alert input.

2. **`IosActionSheet.kt`**:
   - Ensure 14dp squircle container, 56dp action rows, detached Cancel pill with 8dp gap.
   - Add `isChecked: Boolean = false` support (displaying Cupertino checkmark `Icons.Default.Check` for active options).

3. **`SettingsScreen.kt` Inset Grouped Overhaul**:
   - Integrate with `IosLargeTitleScaffold(scrollState = scrollState)`.
   - Implement 4 Inset Grouped sections using `IosListSection`, `IosListRow`, `IosNavigationRow`, and `IosSwitchRow`:
     - Section 1: Cloud & Sync (Supabase sync status, manual sync button)
     - Section 2: Security & Privacy (PIN lock switch, change PIN row, biometric switch)
     - Section 3: Appearance & Style (Theme mode, Font selection)
     - Section 4: Data Management (Import TXT, Export Markdown Zip, Export/Import JSON Backup, Trash)
   - Squircle category icons: 30dp box, 7dp radius, native system colors (Blue, Green, Purple, Orange, Cyan, Red) with 18dp crisp white icons.
   - 56dp indented hairline dividers between rows (`showDivider = true` except last row in section).
   - Spring touch physics (`Modifier.iosClick`) and zero ink ripples.
   - Replace ALL Android `AlertDialog` / `BasicAlertDialog` with `IosModalDialog` and `IosActionSheet`.
   - Ensure `AppLockManager.isPickerActive = true` is set before launching external pickers/share sheets.

## Verification Requirements
You MUST run:
1. `./gradlew compileDebugKotlin`
2. `./gradlew test`
3. `./gradlew assembleDebug`
Document the commands, exact outputs, and pass rates in your handoff report.

Write your report to:
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m4_gen2/report.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m4_gen2/handoff.md`
Send a completion message back to parent when finished.
