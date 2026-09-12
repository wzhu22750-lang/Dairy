## 2026-09-06T12:26:34Z
You are Worker M5 for Milestone 5 (Editor & Secondary Screens Polish).
Working directory: /Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m5
Project root: /Users/kuangqie/Documents/VibeCoding/日记本

MANDATORY READING BEFORE STARTING:
- /Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md
- /Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md
- /Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m5_1/handoff.md
- /Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m5_2/handoff.md
- /Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m5_2/proposed_IosDateTimePickerSheet.kt
- /Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m5_3/handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

File Ownership (EXCLUSIVE WRITE):
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosDateTimePickerSheet.kt` (NEW)
- `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt` (EXCLUSIVE WRITE)
- `app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt` (EXCLUSIVE WRITE)
- `app/src/main/java/com/example/inkpaperdiary/ui/onthisday/OnThisDayScreen.kt` (EXCLUSIVE WRITE)
- `app/src/main/java/com/example/inkpaperdiary/ui/search/SearchScreen.kt` (EXCLUSIVE WRITE)
- `app/src/main/java/com/example/inkpaperdiary/ui/stats/StatsScreen.kt` (EXCLUSIVE WRITE)
- `app/src/main/java/com/example/inkpaperdiary/ui/trash/TrashScreen.kt` (EXCLUSIVE WRITE)
- STRICTLY PROTECTED: All Room DAOs, entities, AppLockManager, PinCipher, SyncManager, and Supabase client. DO NOT touch them.

Tasks to Execute:
1. Create `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosDateTimePickerSheet.kt` based on Explorer M5-2's blueprint and `proposed_IosDateTimePickerSheet.kt`:
   - Modal bottom sheet with 16dp rounded top corners, `AppleMaterials.backgroundColor(MaterialThickness.ULTRA_THICK)`, and 0.5dp specular hairline border.
   - 50dp navigation header with "取消", "选择时间", and "完成" actions strictly satisfying F12/F13 contracts.
   - iOS Segmented Control ("日期", "时间") + live readout banner.
   - Date Tab: 7-column calendar grid, month switcher (< / >), "今天" jump, leap year & Epoch 0L safety.
   - Time Tab: 2-column Cupertino Wheel Picker (Hours 00..23, Minutes 00..59) with snapping and haptics.
   - Include `IosDatePickerSheet` alias for backwards compatibility.
2. Implement Apple HIG architecture in `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt` based on Explorer M5-1's blueprint:
   - iOS 44dp modal navigation bar with "取消" text button (`IosNavTextButton`) on left, date/time capsule pill (`EditorDateTimeCapsulePill`) in center, and "完成" (`IosNavTextButton`, `isPrimary = true`) + Pin toggle on right.
   - Smart dirty checking (`isDirty`): clean state dismisses immediately; dirty state prompts with `IosActionSheet` ("放弃修改" destructive vs "继续编辑" vs "保存并退出"). Apply same logic in `BackHandler`.
   - Date/time capsule pill triggers `IosDateTimePickerSheet`. Completely purge legacy `DatePickerDialog` and `TimePickerDialog`.
   - Markdown formatting toolbar anchored above keyboard (`Modifier.imePadding()`) with 0.5dp specular top border and `IosEditorToolButton` using `Modifier.iosClick`.
   - Replace any remaining `OutlinedTextField` with `IosDialogTextField`, replace clickables with `Modifier.iosClick`, and wrap the screen tree in `SuppressMaterialRipples`.
   - Preserve `AppLockManager.isPickerActive = true` before launching photo picker.
3. Refactor secondary screens according to Explorer M5-3's blueprints:
   - `CalendarScreen.kt`:
     * Standardize top bar to 44dp iOS Navigation Bar with `IosNavBackButton(label = "返回")` (when `onNavigateBack != null`) and centered 17sp SemiBold title.
     * Fix day cell scale from 0.90f to standard 0.97f (`Modifier.iosClick`).
     * Replace Material 3 `Button` in empty state with iOS capsule button (`Surface` + `CapsuleShape` + `Modifier.iosClick`).
     * Bottom padding: `if (onNavigateBack != null) 24.dp else 96.dp`.
   - `OnThisDayScreen.kt`:
     * Eliminate Material 3 `TopAppBar` and `IconButton`.
     * Migrate to `IosLargeTitleScaffold(title = "那年今日")` with `IosLargeTitleItem` and `IosNavBackButton(label = "返回")`.
     * Enforce `SansFontFamily`.
   - `SearchScreen.kt`:
     * Standardize top bar height to 44dp with `IosNavTextButton(text = "取消")`.
     * 36dp search input capsule with translucent background, hairline border, clear button with `iosIconClick`.
     * Enforce `SansFontFamily`.
   - `StatsScreen.kt`:
     * Migrate to `IosLargeTitleScaffold(title = "数据与统计")` with `IosLargeTitleItem` and `IosNavBackButton(label = "返回")`.
     * Enforce `SansFontFamily`.
   - `TrashScreen.kt`:
     * Migrate to `IosLargeTitleScaffold(title = "回收站")` with `IosLargeTitleItem`, `IosNavBackButton(label = "返回")`, and `IosNavTextButton(text = "清空", color = error)`.
     * Add `IosModalDialog` confirmation prompt for single diary permanent deletion.
4. Verification:
   - Run `./gradlew compileDebugKotlin` and verify 0 compiler errors.
   - Run `./gradlew testDebugUnitTest` and verify 100% test pass.
   - Run `./gradlew assembleDebug` and verify build succeeds with 0 errors.
5. Write complete `handoff.md` to `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m5/handoff.md`.
6. Send message to caller with your handoff reference and results summary.
