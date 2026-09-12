# Milestone 3 Implementation Report: Apple Journal Timeline Screen Overhaul

**Agent**: Worker M3 (Gen 2)  
**Target Files**:
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt`  
**Date**: 2026-09-06  
**Status**: COMPLETE (Verified by compiler, unit test suite, and debug APK build)

---

## 1. Executive Summary

Milestone 3 successfully transformed the `TimelineScreen` of InkPaperDiary from an interim Android-style interface into an authentic **Apple Journal-style timeline stream architecture**, strictly conforming to Apple Human Interface Guidelines (HIG) and the design contracts established by Explorers M3-1, M3-2, and M3-3.

Key accomplishments:
1. **Scaffold & Navigation Bar Modernization**: Fully integrated with `IosLargeTitleScaffold(lazyListState = listState)`. Dynamic Chinese date subtitle (e.g. `"9月6日 星期日"`) is placed in the collapsible header above the 34sp Title. Top navigation bar features Search (`onNavigateToSearch`) and Compose (`onNavigateToEditor(null)`) via `IosNavIconButton`.
2. **Material Idiom Purge**: Completely eliminated Android Material FloatingActionButton (FAB), 3-dot overflow menu (`Icons.Default.MoreVert`), and `DropdownMenu`.
3. **`IosSegmentedControl` & Reactive Filtering**: Implemented a 3-segment filter bar ("全部", "图文", "置顶") under the large title, operating on the reactive diary list while preserving Room DAOs and database ordering.
4. **Apple Journal Stream Cards**: Designed `DiaryCardItem` using `PaperCard` with 16dp squircle corners, 0.5dp specular hairline border, left 3dp vertical accent indicator pill for pinned entries, and spring touch compression (`Modifier.iosClick` scale 0.97f, alpha 0.85f, zero ink ripples).
5. **Adaptive Multi-Photo Mosaic (`JournalPhotoMosaic`)**: Replaced horizontal thumbnail scrolling with an adaptive layout:
   - 1 photo: Full-width hero banner (180dp height)
   - 2 photos: Side-by-side 2 equal columns (130dp height)
   - 3 photos: Asymmetrical collage (1.5x large hero on left, 2 vertically stacked on right, 160dp height)
   - 4 photos: 2x2 grid (two rows of 96dp each)
   - 5+ photos: 2x2 grid with 4th slot displaying a dark translucent overlay (`Color.Black.copy(alpha = 0.45f)`) and centered `"+N"` badge
   - All photos framed with 12dp squircle corners and 0.5dp specular glass borders.
6. **Contextual `IosActionSheet`**: Long-pressing a diary card triggers an authentic iOS bottom action sheet with dynamic "置顶此篇"/"取消置顶", "编辑日记", destructive red "移入回收站" (`isDestructive = true`, Apple Red `#FF3B30`), and detached 14dp squircle "取消" pill.
7. **iOS Minimalist Empty State**: Displays a 72dp squircle frosted icon container, Apple HIG typography, and a 44dp capsule CTA button ("新建第一篇日记").
8. **Backward-Compatibility Pop Fix**: Replaced 5 occurrences of `modalStack.removeLast()` with `modalStack.removeAt(modalStack.size - 1)` in `AppNavigation.kt` to guarantee crash-free runtime on Android 8.0 through Android 14.

---

## 2. Detailed Technical Changes

### 2.1 `AppNavigation.kt` Safe Pop Fix
Replaced all 5 occurrences of `modalStack.removeLast()` with `modalStack.removeAt(modalStack.size - 1)`:
- Line 77: BackHandler pop
- Line 115: EditorScreen `onNavigateBack`
- Line 124: SearchScreen `onNavigateBack`
- Line 136: StatsScreen `onNavigateBack`
- Line 145: TrashScreen `onNavigateBack`

This avoids `java.lang.NoSuchMethodError` on devices running Android API < 35 where `java.util.List.removeLast()` is not available on ART.

### 2.2 `TimelineScreen.kt` Overhaul
1. **Scaffold Integration**:
   - Swapped out interim manual Box layout for `IosLargeTitleScaffold(title = "日记", lazyListState = listState, actions = { ... })`.
   - Actions slot contains:
     - `IosNavIconButton` with `Icons.Outlined.Search` -> `onNavigateToSearch`
     - `IosNavIconButton` with `Icons.Outlined.Edit` -> `onNavigateToEditor(null)`
   - Computed `scrollOffset` with `rememberLazyListScrollOffset(listState)` driving `IosLargeTitleItem` inverse alpha fade.
2. **Header Subtitle**:
   - Formatter `SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)` outputs e.g. "9月6日 星期日".
   - Rendered in `IosLargeTitleItem` above the 34sp title in 12sp SemiBold with 0.5sp tracking.
3. **Segmented Filter Bar**:
   - `IosSegmentedControl` rendered with `listOf("全部", "图文", "置顶")`.
   - Connected reactively to `displayedDiaries` via in-memory filtering.
   - Preserved `Mood.entries` capsule filter bar.
4. **Journal Stream Card & Mosaic Layout**:
   - `DiaryCardItem` uses `PaperCard` with `hasCeladonAccent = diary.isPinned`.
   - Typography: 13sp Footnote date, 17sp Headline title, 15sp Subheadline body.
   - Pinned entries display `JournalPinnedBadge` and left 3dp vertical accent bar.
   - `JournalPhotoMosaic` dynamically switches between 1, 2, 3, 4, 5+ layouts with 12dp squircle corners.
5. **Contextual Action Sheet**:
   - Long press on `PaperCard` triggers `selectedDiaryForAction = diary`.
   - Displays `IosActionSheet` with diary title, formatted date, pin/unpin action, edit action, and destructive delete action.
   - Detached "取消" button handles dismissal.
6. **Zero Material Idioms**:
   - Zero `FloatingActionButton`
   - Zero `Icons.Default.MoreVert`
   - Zero `DropdownMenu`

---

## 3. Verification Commands and Results

| Step | Command | Result | Details |
|---|---|---|---|
| Kotlin Compilation | `./gradlew compileDebugKotlin` | **BUILD SUCCESSFUL** | 7 actionable tasks executed, 0 errors |
| Unit Test Suite | `./gradlew test --rerun-tasks` | **BUILD SUCCESSFUL** | 26 actionable tasks executed, 100% tests passed |
| Debug APK Build | `./gradlew assembleDebug` | **BUILD SUCCESSFUL** | Generated `app-debug.apk` (22MB) |
| MoreVert Audit | `grep -rn "Icons.Default.MoreVert" app/src/main/` | **0 occurrences** | Purge verified |
| DropdownMenu Audit | `grep -rn "DropdownMenu" app/src/main/` | **0 occurrences** | Purge verified |
| FAB Audit | `grep -rn "FloatingActionButton" app/src/main/` | **0 occurrences** | Purge verified |
| removeLast Audit | `grep -rn "removeLast" app/src/main/java/com/example/inkpaperdiary/ui/navigation/` | **0 occurrences** | Compatibility verified |
