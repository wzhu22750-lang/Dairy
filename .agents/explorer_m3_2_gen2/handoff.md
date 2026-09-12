# Handoff Report: TimelineScreen Scaffolding & IosSegmentedControl Integration

**From**: Explorer M3-2 (Gen 2)  
**To**: Worker M3  
**Target Files**: 
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineViewModel.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosSegmentedControl.kt`

---

## 1. Observation

1. **Current Scaffolding Implementation** in `TimelineScreen.kt` (lines 123–144 and 309–344):
   - Currently uses a manual root `Box` containing a `LazyColumn` and a floating `IosLargeTitleTopBar`:
     ```kotlin
     Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
         LazyColumn(
             state = listState,
             modifier = Modifier.fillMaxSize(),
             contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp),
             verticalArrangement = Arrangement.spacedBy(12.dp)
         ) {
             item(key = "header_large_title") {
                 IosLargeTitleItem(
                     title = "日记",
                     subtitle = currentDateStr,
                     modifier = Modifier.padding(top = 10.dp)
                 )
             }
             ...
         }
         IosLargeTitleTopBar(title = "日记", scrollOffset = scrollOffset, actions = { ... })
     }
     ```
   - `IosLargeTitleItem` is invoked on line 139 without passing `scrollOffset`, preventing the large title from fading out during scroll (alpha remains permanently 1.0f).
   - `top = 56.dp` is a hardcoded offset that fails to account for dynamic status bar heights across different Android devices.

2. **Standard Scaffold Implementation** in `IosLargeTitleScaffold.kt` (lines 148–179):
   - Already provides a dedicated LazyListState overload:
     ```kotlin
     @Composable
     fun IosLargeTitleScaffold(
         title: String,
         lazyListState: LazyListState,
         modifier: Modifier = Modifier,
         navigationIcon: (@Composable () -> Unit)? = null,
         actions: (@Composable RowScope.() -> Unit)? = null,
         bottomBar: (@Composable () -> Unit)? = null,
         snackbarHost: (@Composable () -> Unit)? = null,
         containerColor: Color = MaterialTheme.colorScheme.background,
         contentColor: Color = MaterialTheme.colorScheme.onBackground,
         scrollThreshold: Dp = IosLargeTitleDefaults.CollapseThresholdDp,
         content: @Composable (PaddingValues) -> Unit
     )
     ```
   - Computes `statusBarTop + 44.dp` and passes it as `innerPadding.calculateTopPadding()`.
   - Also provides `IosNavIconButton` (lines 433–455) for top navigation actions with spring compression (`iosIconClick`) and zero ink ripple.

3. **Current Filter Implementation** in `TimelineScreen.kt` (lines 154–166) & `TimelineViewModel.kt` (lines 24–46):
   - Only provides a 2-item segmented control: `listOf("全部", "置顶")`.
   - `TimelineViewModel` filters only by `_onlyPinned` and `_selectedMood`.
   - Missing the "图文" (Media) filter segment required by Apple HIG Journal specifications.

4. **Zero Material FAB & MoreVert Status**:
   - `grep_search` confirmed 0 occurrences of `FloatingActionButton` across `src/main/java/com/example/inkpaperdiary/ui`.
   - `MaterialIdiomPurgeAuditTest` confirms 0 `FloatingActionButton` imports and passes with code 0 (`./gradlew testDebugUnitTest`).

---

## 2. Logic Chain

1. **Scaffold Unification**:
   - From Observation 1 & 2: By replacing the manual `Box` + `IosLargeTitleTopBar` with `IosLargeTitleScaffold(title = "日记", lazyListState = listState, actions = { ... })`, the top bar is pinned with exact status bar insets (`statusBarTop + 44.dp`).
   - Supplying `scrollOffset` to `IosLargeTitleItem` enables the mathematical inverse-alpha formula `(1f - (scrollOffset / thresholdPx)).coerceIn(0f, 1f)`, causing the 34sp title to fade out smoothly over 52dp as the 17sp inline title in the top bar fades in.

2. **Edge-to-Edge Content Viewport**:
   - Applying `contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 8.dp, bottom = 88.dp)` to `LazyColumn` ensures the diary card feed scrolls *behind* the 93% frosted glass navigation bar rather than being abruptly clipped.
   - The `88.dp` bottom padding provides clearance over the `49.dp` + navigation bar insets of the bottom `IosTabBar`.

3. **Date Subtitle Formatting**:
   - Formatting `Date()` via `SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)` produces strings such as `"9月6日 星期日"`.
   - Placed above the large title in `IosLargeTitleItem`, this mirrors the native Apple Journal / iOS Calendar date header pattern.

4. **Navigation Actions & FAB Eradication**:
   - Wrapping `Icons.Outlined.Search` and `Icons.Outlined.Edit` in `IosNavIconButton` provides authentic 36dp touch targets with 0.96x spring scale feedback, zero ink ripple, and primary color tint.
   - Composing diary entries from the top-right navigation bar completely obviates the Android FAB idiom.

5. **Segmented Filter Bar & Reactive Room Flow**:
   - From Observation 3: Introducing `enum class TimelineFilterSegment(val label: String) { ALL("全部"), MEDIA("图文"), PINNED("置顶") }` expands filtering to include media entries (`diary.attachments.isNotEmpty()`).
   - Executing filtering in the ViewModel's `combine` flow preserves the underlying Room `DiaryDao.getAllDiaries()` queries intact, preventing any database regressions while enabling real-time reactive updates upon entry creation or deletion.

---

## 3. Caveats

1. **Dual Implementation Paths for Filter State**:
   - Option A (ViewModel-driven, recommended) updates `TimelineViewModel` and `TimelineUiState`.
   - Option B (UI-driven) performs collection filtering directly in `TimelineContent` via `remember(uiState.filteredDiaries, selectedSegment)`.
   - If Option A is selected, keep `onlyPinned = segment == TimelineFilterSegment.PINNED` and `fun togglePinnedFilter()` for 100% backward compatibility with any legacy test fixtures.
2. **Sticky vs Inline Filter Bar**:
   - The primary blueprint uses an inline header (`item(key = "filter_controls")`) matching Apple Journal. If sticky behavior is desired, replace `item` with `stickyHeader`, adding `Modifier.background(MaterialTheme.colorScheme.background)`.

---

## 4. Conclusion

The timeline scaffolding and filter bar can be fully upgraded to Apple HIG standards without modifying any Room entities or DAOs. The refactoring consists of:
1. Wrapping `TimelineContent` in `IosLargeTitleScaffold(lazyListState = listState)`.
2. Setting trailing actions to Search and Compose `IosNavIconButton`s (zero Android FAB).
3. Passing `scrollOffset` and Chinese date subtitle (`"M月d日 EEEE"`) to `IosLargeTitleItem`.
4. Integrating `IosSegmentedControl` with segments `"全部"`, `"图文"`, and `"置顶"`.
5. Preserving Room flows by executing filtering reactively in memory.

---

## 5. Verification Method

1. **Compilation and Unit Test Execution**:
   Run the test suite to verify zero syntax errors or regressions:
   ```bash
   ./gradlew testDebugUnitTest
   ```
2. **Audit Test Execution**:
   Verify no FloatingActionButton violations exist:
   ```bash
   ./gradlew test --tests "com.example.inkpaperdiary.tier1_features.MaterialIdiomPurgeAuditTest"
   ```
3. **Inspection Checklist for Worker M3**:
   - Inspect `TimelineScreen.kt`: confirm `IosLargeTitleScaffold` wraps the screen and `IosLargeTitleTopBar` is no longer manually instantiated.
   - Inspect `TimelineFilterBar`: confirm 3 segments (`"全部"`, `"图文"`, `"置顶"`) are selectable and trigger haptics with animated pill slider.
   - Inspect top bar: confirm Search and Compose icons are present, and no `FloatingActionButton` exists.
