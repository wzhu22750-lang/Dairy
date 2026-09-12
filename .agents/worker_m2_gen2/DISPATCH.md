# Dispatch — Worker M2 (Gen 2)

## 2026-09-06T19:04:00+08:00
You are Worker M2 (Gen 2) implementing Milestone 2: Root Navigation Architecture & Collapsible Large Title.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md` before starting work.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md` for architecture and interface contracts.
- DO NOT modify non-UI business logic: Room DAOs/entities, Security (`AppLockManager`, `PinCipher`), and Supabase sync must remain 100% untouched.
- DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Write Ownership
You exclusively own and may edit the following files:
1. `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`
2. `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`
3. `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt`

## Inputs & Blueprints from Explorers
Read and implement the comprehensive blueprints prepared by the M2 Explorers:
- Explorer M2-1 Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_1_gen2/report.md`
- Explorer M2-2 Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2_gen2/report.md`
- Explorer M2-3 Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_3_gen2/report.md`

## Required Implementations:
1. **`IosTabBar.kt`**:
   - Exact HIG geometry: 49.dp content height, 24.dp icons, 10.sp label text.
   - 93% translucency background via `AppleMaterials.barBackgroundColor(isDark)`.
   - 0.5.dp specular hairline top border via `AppleMaterials.glassBorder(isDark)`.
   - 4 canonical tabs: `Journal`, `Calendar`, `Memories` (using `Icons.Outlined.History` / `Icons.Filled.History`), `Settings`.
   - Spring touch physics via `Modifier.iosTabClick` (0.92f scale, 0.80f alpha, haptic tick).
   - Active tint (`#007AFF`) / Inactive tint (`#8E8E93`), `Role.Tab` semantics, `WindowInsets.navigationBars`.

2. **`IosLargeTitleScaffold.kt`**:
   - Implement `IosLargeTitleScaffold` composables for `ScrollState`, `LazyListState`, and raw `scrollOffset`.
   - Dynamic collapse threshold: `52.dp.toPx()` via `with(LocalDensity.current) { 52.dp.toPx() }`.
   - Remove the `140f` clamp in `rememberLazyListScrollOffset` and `rememberScrollStateOffset`.
   - Inverse alpha crossfade on `IosLargeTitleItem` (`calculateLargeTitleAlpha = (1f - (scrollOffsetPx / thresholdPx)).coerceIn(0f, 1f)` via `Modifier.graphicsLayer`).
   - Frosted glass elevation (93% translucent bar background) and 0.5.dp hairline bottom divider appearing when collapsed.
   - Action primitives: `IosNavIconButton`, `IosNavTextButton`, `IosNavBackButton`.

3. **`AppNavigation.kt`**:
   - Solidify 2-tier root navigation: persistent 4-tab bar at root (`Journal`, `Calendar`, `Memories`, `Settings`) + modal stack (`Editor`, `Search`, `Stats`, `Trash`).
   - Verify 0 Material FAB and 0 3-dot `MoreVert` / `DropdownMenu`.

## Verification Requirements
You MUST run:
1. `./gradlew compileDebugKotlin`
2. `./gradlew test` (or `./gradlew testDebugUnitTest`)
3. `./gradlew assembleDebug`
Document the commands, exact outputs, and pass rates in your handoff report.

Write your report to:
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m2_gen2/report.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/worker_m2_gen2/handoff.md`
Send a completion message back to parent when finished.
