# Technical Architecture Report: TimelineScreen Scaffolding & IosSegmentedControl Filter Bar

**Author**: Explorer M3-2 (Gen 2)  
**Milestone**: M3 (Timeline Screen Overhaul)  
**Target Component**: `com.example.inkpaperdiary.ui.timeline.TimelineScreen.kt` & `TimelineViewModel.kt`  
**Dependencies**: `IosLargeTitleScaffold.kt`, `IosSegmentedControl.kt`, `IosTouchPhysics.kt`, `AppleMaterial.kt`  
**Status**: COMPLETE

---

## 1. Executive Summary

Milestone 3 focuses on transforming the diary timeline into an authentic Apple Journal stream. This report provides the architectural blueprint and drop-in implementations for:
1. **Full Integration with `IosLargeTitleScaffold`**: Transitioning from an interim manual `Box` overlay to the standard `IosLargeTitleScaffold(lazyListState = listState)` architecture with 52dp scroll collapse threshold, dynamic frosted glass elevation, and inverse-alpha crossfade between the 34sp Bold Large Title and the 17sp SemiBold Inline Title.
2. **Dynamic Date Subtitle**: Formatting the current system date as `"M月d日 EEEE"` in Chinese locale (e.g., `"9月6日 星期日"`), positioned natively above the 34sp Title in 12sp SemiBold uppercase with 0.5sp tracking.
3. **Top Navigation Actions & Zero Android FAB**: Placing Search (`Icons.Outlined.Search`) and Compose (`Icons.Outlined.Edit`) icon buttons in the top navigation bar trailing slot using `IosNavIconButton` (spring compression, haptic tick, zero ink ripple). Absolute zero `FloatingActionButton` exists in the layout.
4. **`IosSegmentedControl` Filter Integration**: Implementing a 3-segment filter bar ("全部", "图文", "置顶") featuring an animated floating pill slider (7dp squircle), 0.5dp specular hairline dividers, and haptic feedback.
5. **Reactive Room Flow Preservation**: Ensuring 100% preservation of Room DAOs (`getAllDiaries()`) without SQL mutations by executing filtering reactively in memory via Kotlin Flow `combine`.

---

## 2. Scaffolding Architecture & `IosLargeTitleScaffold` Integration

### 2.1 Current State Analysis
In the current `TimelineScreen.kt` (lines 123–345):
```kotlin
// Legacy / interim scaffolding approach in TimelineScreen.kt:
Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)
    ) {
        item(key = "header_large_title") {
            IosLargeTitleItem(title = "日记", subtitle = currentDateStr) // Missing scrollOffset!
        }
        ...
    }
    IosLargeTitleTopBar(title = "日记", scrollOffset = scrollOffset, actions = { ... })
}
```
**Deficiencies Identified**:
1. `IosLargeTitleScaffold` (standardized in M2) is bypassed; `TimelineScreen` manually manages layout Box, window insets, and top bar placement.
2. `IosLargeTitleItem` is not supplied with `scrollOffset`, meaning the large 34sp title does NOT fade out on scroll (`alpha` stays 1.0f).
3. The top padding of `56.dp` is a hardcoded magic number that ignores dynamic status bar insets on various physical Android devices.

### 2.2 Target Apple HIG Architecture
Using `IosLargeTitleScaffold(lazyListState = listState)`:
1. **Window Inset Awareness**: Automatically computes `topBarTotalHeight = statusBarTop + 44.dp`, passing it to the child content as `innerPadding`.
2. **Edge-to-Edge Scrolling**: `LazyColumn` takes `modifier = Modifier.fillMaxSize()` and `contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 8.dp, bottom = 88.dp)`. The list items visually scroll *behind* the frosted glass top bar rather than being clipped below it.
3. **Coordinated Alpha Fading**:
   - `scrollOffset` is computed via `rememberLazyListScrollOffset(listState, thresholdPx)`.
   - When scrolled between `0` and `52dp` (`CollapseThresholdDp`), the large title in `IosLargeTitleItem` fades from `1.0f` to `0.0f` (`1f - (scrollOffset / thresholdPx)`).
   - Simultaneously, `IosLargeTitleTopBar` inline title fades from `0.0f` to `1.0f` (`scrollOffset / thresholdPx`), and the bar background smoothly transitions to 93% frosted glass (`AppleMaterials.barBackgroundColor`).

```
Resting State (ScrollOffset = 0):
┌──────────────────────────────────────────────────────────┐
│ [Status Bar] (Transparent)                               │
│ [Navigation Bar: 44dp] (Transparent, Inline Title Alpha:0)│
│   Actions: [Search (36dp)]  [Compose (36dp)]             │
├──────────────────────────────────────────────────────────┤
│ 9月6日 星期日 (12sp SemiBold MonoGray500)                 │
│ 日记 (34sp Bold, Alpha: 1.0)                              │
│                                                          │
│ [全部 | 图文 | 置顶] (IosSegmentedControl 32dp)           │
│ (● 全部心情) (● 开心) (● 平静) ... (Mood Capsules)        │
│                                                          │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Diary Card 1 (16dp squircle, 0.5dp glassBorder)      │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘

Collapsed State (ScrollOffset >= 52dp):
┌──────────────────────────────────────────────────────────┐
│ [Status Bar] (Frosted Glass 93% alpha)                   │
│ [Navigation Bar: 44dp] (Frosted Glass 93% alpha)         │
│   Centered Title: "日记" (17sp SemiBold, Alpha: 1.0)      │
│   Actions: [Search (36dp)]  [Compose (36dp)]             │
│ ──────────────────────────────────────────────────────── │ (0.5dp Hairline Border)
│                                                          │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Diary Card 3 (scrolled beneath top bar)              │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## 3. Dynamic Date Subtitle & Header Typography

### 3.1 Date Formatting Specification
- **Pattern**: `"M月d日 EEEE"`
- **Locale**: `Locale.CHINESE`
- **Output Sample**: `"9月6日 星期日"`
- **Implementation**:
```kotlin
val todayFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
val currentDateStr = remember { todayFormatter.format(Date()) }
```

### 3.2 Typography Hierarchy
- **Date Subtitle**: 12sp, SemiBold, `SansFontFamily`, `PaperColors.MonoGray500`, uppercase, letter spacing `0.5.sp`.
- **Large Screen Title**: 34sp, Bold, `SansFontFamily`, line height `41.sp`, letter spacing `-0.4.sp`, `MaterialTheme.colorScheme.onSurface`.
- **Inline Navigation Title**: 17sp, SemiBold, `SansFontFamily`, line height `22.sp`, letter spacing `-0.4.sp`, centered.

---

## 4. Top Navigation Actions & Zero Android FAB

### 4.1 Navigation Actions Specification
In Apple HIG, primary creation actions are located in the navigation bar rather than floating over content.
- **Search Action**:
  - Icon: `Icons.Outlined.Search`
  - Touch target: `36.dp` square box with `22.dp` icon glyph
  - Visual feedback: `Modifier.iosIconClick` (0.96x spring scale, 0.85x alpha, zero ink ripple)
  - Navigation: `onNavigateToSearch()`
- **Compose Action**:
  - Icon: `Icons.Outlined.Edit`
  - Touch target: `36.dp` square box with `22.dp` icon glyph
  - Visual feedback: `Modifier.iosIconClick`
  - Navigation: `onNavigateToEditor(null)`

### 4.2 Absolute Elimination of Android FAB
- **Audit Verification**: Codebase audit (`MaterialIdiomPurgeAuditTest`) verifies 0 imports of `androidx.compose.material3.FloatingActionButton` across all UI packages.
- **Empty State Creation**: When the timeline contains 0 diaries, an Apple HIG centered capsule button ("新建第一篇日记") provides an intuitive in-stream call to action with spring touch feedback.

---

## 5. `IosSegmentedControl` Filter Bar Architecture

### 5.1 Segment Model
```kotlin
enum class TimelineFilterSegment(val label: String) {
    ALL("全部"),
    MEDIA("图文"),
    PINNED("置顶")
}
```

### 5.2 Geometry & Visual Styling (HIG Compliant)
1. **Outer Track**:
   - Height: `32.dp`
   - Shape: `RoundedCornerShape(9.dp)` (squircle approximation)
   - Background: Light `Color(0xFFE5E5EA)`, Dark `Color(0xFF1C1C1E)`
   - Border: `0.5.dp` hairline specular border
   - Inner padding: `2.dp`
2. **Floating Pill Thumb**:
   - Height: `28.dp` (`32.dp - 2 * 2.dp`)
   - Shape: `RoundedCornerShape(7.dp)`
   - Background: Light `Color.White`, Dark `Color(0xFF636366)`
   - Elevation: `2.dp` subtle soft shadow
   - Physics: Animated offset driven by `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)`
3. **Micro Hairline Dividers**:
   - Width: `0.5.dp`
   - Height: `22.dp` (vertical padding `5.dp`)
   - Fades to transparent when adjacent to the active pill thumb
4. **Interaction**:
   - Zero ripple (`indication = null`)
   - Haptic click feedback on change: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`

### 5.3 Inline vs Sticky Header Placement
| Aspect | Inline Header (`item`) | Sticky Header (`stickyHeader`) |
|---|---|---|
| **Apple Journal Alignment** | Authentic (matches iOS Journal stream) | Deviates (used for Section Tabs, e.g. Music) |
| **Screen Real Estate** | Maximizes vertical reading area when scrolling | Consumes ~130dp permanent vertical space |
| **Overlapping Behavior** | Flawless scroll under frosted top bar | Requires opaque background to avoid card ghosting |
| **Recommendation** | **PRIMARY ARCHITECTURE (Inline)** | Alternate configuration provided below |

---

## 6. Reactive Flow & Room Data Protection

### 6.1 Core Principle
Room database entities (`DiaryEntity`, `AttachmentEntity`) and DAOs (`DiaryDao`) must remain **100% UNTOUCHED**.
`DiaryDao.getAllDiaries()` emits `Flow<List<DiaryWithDetails>>`. The repository maps this to `Flow<List<Diary>>`.
Filtering is executed entirely in memory:
1. `TimelineFilterSegment.ALL`: passes all diaries.
2. `TimelineFilterSegment.MEDIA`: filters `diary.attachments.isNotEmpty()`.
3. `TimelineFilterSegment.PINNED`: filters `diary.isPinned`.
4. Secondary Mood Filter: filters `diary.mood == selectedMood`.

Sorting order (`isPinned DESC, entryDate DESC`) is preserved because Kotlin `List.filter` retains original collection ordering.

### 6.2 ViewModel State Machine Blueprint
```kotlin
class TimelineViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _selectedFilterSegment = MutableStateFlow(TimelineFilterSegment.ALL)
    private val _selectedMood = MutableStateFlow<Mood?>(null)

    val uiState: StateFlow<TimelineUiState> = combine(
        repository.getAllDiaries(),
        _selectedFilterSegment,
        _selectedMood
    ) { diaries, segment, moodFilter ->
        var filtered = diaries
        when (segment) {
            TimelineFilterSegment.ALL -> { /* no-op */ }
            TimelineFilterSegment.MEDIA -> {
                filtered = filtered.filter { it.attachments.isNotEmpty() }
            }
            TimelineFilterSegment.PINNED -> {
                filtered = filtered.filter { it.isPinned }
            }
        }
        if (moodFilter != null) {
            filtered = filtered.filter { it.mood == moodFilter }
        }

        TimelineUiState(
            diaries = diaries,
            filteredDiaries = filtered,
            selectedFilterSegment = segment,
            selectedMoodFilter = moodFilter,
            onlyPinned = segment == TimelineFilterSegment.PINNED,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TimelineUiState(isLoading = true)
    )

    fun setFilterSegment(segment: TimelineFilterSegment) {
        _selectedFilterSegment.value = segment
    }

    fun togglePinnedFilter() {
        _selectedFilterSegment.value = if (_selectedFilterSegment.value == TimelineFilterSegment.PINNED) {
            TimelineFilterSegment.ALL
        } else {
            TimelineFilterSegment.PINNED
        }
    }

    fun setMoodFilter(mood: Mood?) {
        _selectedMood.value = if (_selectedMood.value == mood) null else mood
    }

    fun togglePin(diary: Diary) {
        viewModelScope.launch { repository.togglePin(diary.id) }
    }

    fun deleteDiary(diaryId: String) {
        viewModelScope.launch { repository.softDeleteDiary(diaryId) }
    }
}
```

---

## 7. Drop-in Composables for Worker M3

### 7.1 Filter Bar Component (`TimelineFilterBar.kt` or inline in `TimelineScreen.kt`)
```kotlin
package com.example.inkpaperdiary.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inkpaperdiary.core.designsystem.AppleMaterials
import com.example.inkpaperdiary.core.designsystem.CapsuleShape
import com.example.inkpaperdiary.core.designsystem.SansFontFamily
import com.example.inkpaperdiary.core.designsystem.components.IosSegmentedControl
import com.example.inkpaperdiary.core.designsystem.components.MoodIcon
import com.example.inkpaperdiary.core.designsystem.interaction.iosClick
import com.example.inkpaperdiary.domain.model.Mood

enum class TimelineFilterSegment(val label: String) {
    ALL("全部"),
    MEDIA("图文"),
    PINNED("置顶")
}

@Composable
fun TimelineFilterBar(
    selectedSegment: TimelineFilterSegment,
    onSegmentSelected: (TimelineFilterSegment) -> Unit,
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Apple HIG Sliding Pill Segmented Control
        IosSegmentedControl(
            items = TimelineFilterSegment.entries,
            selectedItem = selectedSegment,
            onItemSelected = onSegmentSelected,
            itemLabel = { it.label },
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Horizontal Mood Capsule Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                val isAllMoods = selectedMood == null
                Surface(
                    modifier = Modifier
                        .height(28.dp)
                        .iosClick { onMoodSelected(null) },
                    shape = CapsuleShape,
                    color = if (isAllMoods) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = AppleMaterials.glassBorder(width = 0.5.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "全部心情",
                            fontSize = 11.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = if (isAllMoods) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isAllMoods) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(Mood.entries) { mood ->
                val isSelected = selectedMood == mood
                Surface(
                    modifier = Modifier
                        .height(28.dp)
                        .iosClick { onMoodSelected(mood) },
                    shape = CapsuleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = AppleMaterials.glassBorder(width = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MoodIcon(
                            mood = mood,
                            modifier = Modifier.size(12.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else mood.tintColor
                        )
                        Text(
                            text = mood.displayName,
                            fontSize = 11.sp,
                            fontFamily = SansFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
```

### 7.2 Refactored `TimelineContent` Scaffolding
```kotlin
@Composable
fun TimelineContent(
    uiState: TimelineUiState,
    onFilterSegmentChanged: (TimelineFilterSegment) -> Unit,
    onSetMoodFilter: (Mood?) -> Unit,
    onTogglePin: (Diary) -> Unit,
    onDeleteDiary: (String) -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToOnThisDay: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onImportTxt: () -> Unit = {}
) {
    val todayFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    val currentDateStr = remember { todayFormatter.format(Date()) }

    val listState = rememberLazyListState()
    val scrollOffset = rememberLazyListScrollOffset(listState)

    // Contextual Action Sheet state (Managed in coordination with Explorer M3-3)
    var selectedDiaryForAction by remember { mutableStateOf<Diary?>(null) }

    IosLargeTitleScaffold(
        title = "日记",
        lazyListState = listState,
        actions = {
            IosNavIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToSearch
            )
            IosNavIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = "新建日记",
                tint = MaterialTheme.colorScheme.primary,
                onClick = { onNavigateToEditor(null) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 88.dp // Space to clear bottom IosTabBar (49dp + insets)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Collapsible Large Title (fades out over 52dp collapse threshold)
            item(key = "header_large_title") {
                IosLargeTitleItem(
                    title = "日记",
                    subtitle = currentDateStr,
                    scrollOffset = scrollOffset,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 2. Sliding Pill Segmented Control + Mood Filter Capsules
            item(key = "filter_controls") {
                TimelineFilterBar(
                    selectedSegment = uiState.selectedFilterSegment,
                    onSegmentSelected = onFilterSegmentChanged,
                    selectedMood = uiState.selectedMoodFilter,
                    onMoodSelected = onSetMoodFilter
                )
            }

            // 3. Apple Journal stream card list or empty state
            if (uiState.filteredDiaries.isEmpty()) {
                item(key = "empty_state") {
                    TimelineEmptyState(
                        isCompletelyEmpty = uiState.diaries.isEmpty(),
                        onCreateFirstDiary = { onNavigateToEditor(null) }
                    )
                }
            } else {
                items(uiState.filteredDiaries, key = { it.id }) { diary ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DiaryCardItem(
                            diary = diary,
                            onClick = { onNavigateToEditor(diary.id) },
                            onLongClick = { selectedDiaryForAction = diary }
                        )
                    }
                }
            }
        }

        // Long-press Contextual Action Sheet
        val actionDiary = selectedDiaryForAction
        if (actionDiary != null) {
            IosActionSheet(
                visible = true,
                title = actionDiary.title.ifBlank { "日记操作" },
                message = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(actionDiary.entryDate)),
                actions = listOf(
                    IosActionItem(
                        title = if (actionDiary.isPinned) "取消置顶" else "置顶此篇",
                        icon = if (actionDiary.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        onClick = { onTogglePin(actionDiary) }
                    ),
                    IosActionItem(
                        title = "编辑日记",
                        icon = Icons.Outlined.Edit,
                        onClick = { onNavigateToEditor(actionDiary.id) }
                    ),
                    IosActionItem(
                        title = "移入回收站",
                        icon = Icons.Outlined.Delete,
                        isDestructive = true,
                        onClick = { onDeleteDiary(actionDiary.id) }
                    )
                ),
                onDismissRequest = { selectedDiaryForAction = null }
            )
        }
    }
}
```

---

## 8. Verification Strategy & Audit Compliance

1. **Static Analysis & Compilation**:
   - Execute `./gradlew testDebugUnitTest` and verify 0 compiler/linter warnings.
2. **Material Purge Audit**:
   - Confirm `MaterialIdiomPurgeAuditTest` continues to pass with 0 FloatingActionButton violations and 0 MoreVert occurrences in TimelineScreen.
3. **Dynamic Scroll Verification**:
   - At scroll offset = 0, top bar is clear, large title (34sp) is fully opaque.
   - At scroll offset >= 52dp, top bar is frosted glass, inline title (17sp) is fully opaque, large title is alpha 0.
4. **Reactive Filter Verification**:
   - Tap "图文" -> only diaries with non-empty `attachments` are displayed.
   - Tap "置顶" -> only pinned diaries are displayed.
   - Tap "全部" -> all diaries displayed in original Room sort order (`isPinned DESC, entryDate DESC`).
   - Adding, editing, or deleting diaries updates the view reactively via the underlying Room database Flow.
