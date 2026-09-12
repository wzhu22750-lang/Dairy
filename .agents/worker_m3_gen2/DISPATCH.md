# Dispatch — Worker M3 (Gen 2)

## 2026-09-06T19:17:30+08:00
You are Worker M3 (Gen 2) implementing Milestone 3: Timeline Screen Overhaul.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator_gen2/PROJECT.md`.
- DO NOT modify non-UI business logic: Room DAOs/entities, Security (`AppLockManager`, `PinCipher`), and Supabase sync must remain 100% untouched.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Write Ownership
You exclusively own and may edit the following files:
1. `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt`
2. `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt` (for the safe `removeAt` pop fix)

## Inputs & Blueprints from Explorers
Read and integrate the blueprints from the M3 Explorers:
- Explorer M3-1: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m3_1_gen2/report.md`
- Explorer M3-2: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m3_2_gen2/report.md`
- Explorer M3-3: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m3_3_gen2/report.md`

## Required Implementations:
1. **`TimelineScreen.kt` Overhaul**:
   - Integrate with `IosLargeTitleScaffold(lazyListState = listState)`.
   - Title: "日记", Subtitle: dynamic current date string (e.g. "9月6日 星期日").
   - Top-right navigation bar actions: Search (`Icons.Outlined.Search` -> `onNavigateToSearch`) and Compose (`Icons.Outlined.Edit` -> `onNavigateToEditor(null)`).
   - Eradicate 100% of Android Material FAB and 3-dot `MoreVert` / `DropdownMenu`.
   - Implement `IosSegmentedControl` filter bar ("全部", "图文", "置顶") under the large title with reactive list filtering.
   - Implement Apple Journal-style cards: 16dp squircle corners, 0.5dp specular hairline border, left 3dp vertical accent bar, spring compression (`Modifier.iosClick` scale 0.97f, alpha 0.85f, zero ink ripples).
   - Adaptive multi-photo mosaic grid (`JournalPhotoMosaic`) for 1, 2, 3, 4, 5+ photos with 12dp squircle corners and hairline borders.
   - Contextual `IosActionSheet` triggered on card long-press: "置顶此篇"/"取消置顶", "编辑日记", "移入回收站" (destructive red tint), and detached Cancel pill.
   - iOS-style empty state with 72dp squircle frosted icon and capsule CTA button.

2. **`AppNavigation.kt` Safe Pop Fix**:
   - In `AppNavigation.kt`, replace `modalStack.removeLast()` with `modalStack.removeAt(modalStack.size - 1)` to guarantee compatibility across all Android API levels.

## Verification Requirements
You MUST run:
1. `./gradlew compileDebugKotlin`
2. `./gradlew test` (or `./gradlew testDebugUnitTest`)
3. `./gradlew assembleDebug`
Document the commands, exact outputs, and pass rates in your handoff report.

Write your report to:
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m3_gen2/report.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m3_gen2/handoff.md`
Send a completion message back to parent when finished.
