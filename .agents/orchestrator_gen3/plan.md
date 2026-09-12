# Execution Plan — Orchestrator Generation 3

## Overview
Drive the completion of the Apple HIG refactoring of the Jetpack Compose Diary app (`com.example.inkpaperdiary`), executing Milestones 4, 5, and 6 through full iteration loops.

## Milestone 4: Settings Screen & Modal Sheets/Dialogs
### Scope
1. Rebuild `SettingsScreen.kt` using `IosLargeTitleScaffold` (collapsible large title "设置").
2. 4 Inset Grouped sections (`IosListSection` with 16dp squircle and `AppleMaterials.secondarySystemGroupedBackground`):
   - **Section 1 (Cloud & Sync)**: Supabase cloud sync switch/trigger, status indicators, backup/restore navigation.
   - **Section 2 (Security & Privacy)**: PIN / Biometric lock switch (`AppLockManager`), auto-lock timer option.
   - **Section 3 (Appearance & Typography)**: Theme mode (System / Light / Dark via `IosActionSheet`), font selection via `IosActionSheet`.
   - **Section 4 (Data & About)**: Trash / Recycle bin navigation, Clear Cache dialog (`IosModalDialog`), App version info.
3. Replace all Android Material AlertDialogs and BottomSheets with `IosModalDialog` (270dp fixed width, centered, hair-line divider, destructive red action) and `IosActionSheet` (sheet with separate cancel pill).
4. Squircle category icons: 30dp x 30dp box, 7dp corner radius, vibrant iOS system colors (e.g. SystemBlue, SystemGreen, SystemOrange, SystemPurple, SystemGray).
5. Indented dividers: 56dp start indent, matching iOS standard (16dp margin + 30dp icon + 10dp gap).
6. 100% preservation of `SettingsViewModel`, `PreferencesManager`, `AppLockManager`, Room, Supabase sync.

### Iteration Loop for M4
1. **Exploration**: 3 Explorers in parallel:
   - Explorer M4-1: Settings screen structure, existing UI vs iOS Inset Grouped hierarchy.
   - Explorer M4-2: `IosModalDialog.kt` and `IosActionSheet.kt` implementation & integration.
   - Explorer M4-3: ViewModels and Preferences data binding preservation (ensuring zero business regression).
2. **Worker**: 1 Worker to implement components, refactor SettingsScreen, run build and unit tests.
3. **Review**: 2 Reviewers in parallel.
4. **Challenge**: 2 Challengers in parallel (verifying edge cases, layout, non-UI safety).
5. **Audit**: 1 Forensic Auditor (`teamwork_preview_auditor`).
6. **Gate**: Evaluate all verdicts -> PASS -> proceed to Milestone 5.

## Milestone 5: Editor & Secondary Screens Polish
### Scope
1. `EditorScreen.kt`:
   - iOS navigation bar: Cancel / Done text action buttons (`IosNavTextButton`).
   - Inline capsule date/time picker pill displaying formatted date & time.
   - `IosDateTimePickerSheet`: iOS style date/time picker modal replacing Android DatePickerDialog/TimePickerDialog.
   - Markdown formatting toolbar with spring touch feedback (`Modifier.iosClick`).
2. Secondary screens (`CalendarScreen`, `OnThisDayScreen`, `SearchScreen`, `StatsScreen`, `TrashScreen`):
   - Integrate with `IosLargeTitleScaffold` or standard iOS navigation header with `IosNavBackButton`.
   - Apply spring touch feedback (`Modifier.iosClick`) across all list items, cards, and buttons.
   - Zero Material ink ripples, zero FABs, zero 3-dot overflow menus.

### Iteration Loop for M5
1. **Exploration**: 3 Explorers.
2. **Worker**: 1 Worker.
3. **Review**: 2 Reviewers.
4. **Challenge**: 2 Challengers.
5. **Audit**: 1 Forensic Auditor.
6. **Gate**: Evaluate all verdicts -> PASS -> proceed to Milestone 6.

## Milestone 6: Final Verification & Coverage Hardening
### Scope
1. 100% pass of all unit and UI/integration tests.
2. Verify `./gradlew assembleDebug` compiles with 0 errors.
3. Tier 5 Adversarial Coverage Hardening (Challenger stress testing edge cases).
4. Comprehensive Forensic Audit across the entire codebase (zero mock facades, zero hardcoded test fixtures, zero non-UI regressions).
5. Report final success to Sentinel.
