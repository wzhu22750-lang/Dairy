# Handoff Report: Milestone 5-3 (Secondary Screens Apple HIG Polish)

## 1. Observation

### 1.1 Project Context & Baseline Audit
- **Project Root**: `/Users/kuangqie/Documents/VibeCoding/日记本`
- **Baseline Test Command**: `./gradlew testDebugUnitTest`
  - Result: `BUILD SUCCESSFUL in 592ms`, 26 actionable tasks up-to-date, 0 test failures.
- **Material 3 Purge Audit**:
  - `grep_search` across `app/src/main/java` for `MoreVert`: 0 occurrences found.
  - `grep_search` across `app/src/main/java` for `FloatingActionButton`: 0 occurrences found.
  - `grep_search` across `app/src/main/java` for `AlertDialog`: 0 occurrences found.
  - Legacy `TopAppBar`, `IconButton`, and `Button` remain in secondary screens as detailed below.

---

### 1.2 Screen-by-Screen Code Observations

#### Screen 1: CalendarScreen (`app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt`)
1. **Top Navigation Header** (Lines 67–111):
   - Scaffold top bar uses an ad-hoc 52dp `Surface` with `statusBarsPadding()`, `AppleMaterials.backgroundColor(REGULAR)`, and hairline glass border.
   - Back button (Lines 84–96):
     ```kotlin
     Box(
         modifier = Modifier
             .size(40.dp)
             .iosIconClick(onClick = onNavigateBack),
         contentAlignment = Alignment.Center
     ) {
         Icon(
             Icons.AutoMirrored.Filled.ArrowBack,
             contentDescription = "返回",
             tint = MaterialTheme.colorScheme.primary,
             modifier = Modifier.size(20.dp)
         )
     }
     ```
     Observation: Does not use `IosNavBackButton(label = "返回")` and is missing standard HIG back chevron/label alignment.
   - Title (Lines 100–109): Uses start-aligned `Text("日历", style = MaterialTheme.typography.titleMedium)` instead of iOS standard 17sp SemiBold centered title.
2. **Spring Touch Feedback** (Lines 203–211):
   ```kotlin
   Box(
       modifier = Modifier
           .aspectRatio(1f)
           .clip(cellShape)
           .background(
               if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
           )
           .iosClick(pressedScale = 0.90f) { viewModel.selectDate(dayNum) },
       contentAlignment = Alignment.Center
   )
   ```
   Observation: `pressedScale = 0.90f` is an exaggerated scale factor causing visual distortion on 36dp circular day cells. The HIG standard in `IosTouchDefaults.PRESSED_SCALE` is `0.97f`.
3. **Android Material Button Violation** (Lines 287–297):
   ```kotlin
   Button(
       onClick = { onNavigateToEditor(null, uiState.selectedDate.timeInMillis) },
       shape = com.example.inkpaperdiary.core.designsystem.CapsuleShape,
       colors = ButtonDefaults.buttonColors(
           containerColor = MaterialTheme.colorScheme.primary,
           contentColor = MaterialTheme.colorScheme.onPrimary
       ),
       elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
   ) {
       Text("写这天的日记", fontFamily = com.example.inkpaperdiary.core.designsystem.SansFontFamily, fontWeight = FontWeight.SemiBold)
   }
   ```
   Observation: Raw Material 3 `Button` component containing Android ink ripple and lacking iOS spring touch physics.
4. **Diary Cards** (Lines 308–311):
   - `PaperCard(onClick = { onNavigateToEditor(diary.id, null) })`: Correctly applies `Modifier.iosClick`.

---

#### Screen 2: OnThisDayScreen (`app/src/main/java/com/example/inkpaperdiary/ui/onthisday/OnThisDayScreen.kt`)
1. **Material 3 TopAppBar Violation** (Lines 38–67):
   ```kotlin
   TopAppBar(
       title = {
           Row(
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.spacedBy(8.dp)
           ) {
               Text(
                   text = "那年今日",
                   style = MaterialTheme.typography.titleLarge,
                   fontWeight = FontWeight.Bold
               )
               Text(
                   text = todayStr,
                   style = MaterialTheme.typography.bodyMedium,
                   color = MaterialTheme.colorScheme.secondary
               )
           }
       },
       navigationIcon = {
           if (onNavigateBack != null) {
               IconButton(onClick = onNavigateBack) {
                   Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
               }
           }
       },
       colors = TopAppBarDefaults.topAppBarColors(
           containerColor = MaterialTheme.colorScheme.background
       )
   )
   ```
   Observation:
   - Uses Android Material 3 `TopAppBar`.
   - Uses Android Material 3 `IconButton` with ripple effect.
   - Missing `IosNavBackButton(label = "返回")`.
   - Missing `IosLargeTitleScaffold` (34sp Large Title collapsing to 17sp Inline Title).
2. **Typography**: Lines 46–54, 98–108 lack `fontFamily = SansFontFamily`.
3. **Diary Cards** (Line 121): `PaperCard(onClick = { onNavigateToEditor(diary.id) })` correctly implements `iosClick`.

---

#### Screen 3: SearchScreen (`app/src/main/java/com/example/inkpaperdiary/ui/search/SearchScreen.kt`)
1. **Top Bar Structure** (Lines 50–155):
   - Ad-hoc 56dp height header with `statusBarsPadding()`.
   - Cancel action (Lines 139–152):
     ```kotlin
     Box(
         modifier = Modifier
             .iosClick(onClick = onNavigateBack)
             .padding(horizontal = 4.dp, vertical = 6.dp),
         contentAlignment = Alignment.Center
     ) {
         Text(
             text = "取消",
             fontFamily = SansFontFamily,
             color = MaterialTheme.colorScheme.primary,
             fontWeight = FontWeight.Medium,
             fontSize = 16.sp
         )
     }
     ```
     Observation: Manual `Box` implementation instead of standardized `IosNavTextButton(text = "取消", onClick = onNavigateBack)` (17sp SansFontFamily, 36dp min touch target).
2. **Filter Elements**:
   - `IosFilterPill` (Line 324) correctly implements `.iosClick(onClick = onClick)`.
   - `TagChip` (Lines 220–225) correctly implements `.iosClick(onClick = onClick)`.
   - Search results cards (`PaperCard`, Line 262) correctly implement `.iosClick`.
3. **Typography**: Lines 274, 287, 294 in result cards lack `fontFamily = SansFontFamily`.

---

#### Screen 4: StatsScreen (`app/src/main/java/com/example/inkpaperdiary/ui/stats/StatsScreen.kt`)
1. **Top Navigation Header** (Lines 44–84):
   - Ad-hoc 52dp `Surface` top bar.
   - Back button (Lines 60–72): `Box.iosIconClick { onNavigateBack() }` with `ArrowBack`, missing `IosNavBackButton(label = "返回")`.
   - Page container (Lines 89–95):
     `Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()))`
     Observation: A scrollable page that does not utilize `IosLargeTitleScaffold` or `IosLargeTitleItem`.
2. **Component Interactions**:
   - `StatNumberCard` (Lines 238–274) wraps `PaperCard(modifier = modifier)` without `onClick`, rendering static metric tiles.
   - Mood distribution and Tag cloud cards are non-clickable `PaperCard` surfaces.
3. **Typography**: Missing `fontFamily = SansFontFamily` on stat titles, units, and headers.

---

#### Screen 5: TrashScreen (`app/src/main/java/com/example/inkpaperdiary/ui/trash/TrashScreen.kt`)
1. **Top Navigation Header** (Lines 38–96):
   - Ad-hoc 52dp `Surface` top bar with raw `ArrowBack` icon in a 40dp box.
   - Clear action button (Lines 79–93): Manual `Box` with `.iosClick` and `Text("清空")` instead of `IosNavTextButton(text = "清空", color = MaterialTheme.colorScheme.error, isPrimary = true, onClick = ...)`.
   - List container (Lines 133–139): `LazyColumn` without `IosLargeTitleScaffold` or `IosLargeTitleItem`.
2. **Spring Touch Feedback**:
   - Card restore and permanent delete actions (Lines 163–190) use `iosIconClick`.
3. **Data Safety / HIG Violation** (Lines 176–189):
   ```kotlin
   Box(
       modifier = Modifier
           .size(36.dp)
           .iosIconClick { viewModel.permanentDelete(diary.id) },
       contentAlignment = Alignment.Center
   ) {
       Icon(Icons.Outlined.DeleteForever, ...)
   }
   ```
   Observation: Tapping "永久删除" on a single diary directly destroys data without an `IosModalDialog` confirmation prompt, violating Apple HIG destructive action confirmation guidelines. (Batch "清空" correctly has `IosModalDialog`).

---

## 2. Logic Chain

1. **Header & Navigation Uniformity**:
   - *Observation*: `OnThisDayScreen.kt:39` uses `TopAppBar` + `IconButton`; `CalendarScreen.kt:67`, `StatsScreen.kt:44`, and `TrashScreen.kt:39` use ad-hoc 52dp `Surface` bars with raw `ArrowBack`; none of them provide `IosNavBackButton(label = "返回")`.
   - *Inference*: The project standardizes on `IosLargeTitleScaffold` and `IosNavBackButton` (as proven by `SettingsScreen.kt` and `TimelineScreen.kt`).
   - *Conclusion*:
     - `OnThisDayScreen`, `StatsScreen`, and `TrashScreen` are scrollable views and must be migrated to `IosLargeTitleScaffold` with `IosLargeTitleItem` and `IosNavBackButton(label = "返回")`.
     - `CalendarScreen` (due to fixed month switcher + grid layout) must use a standardized 44dp iOS Navigation Bar with `IosNavBackButton(label = "返回")` and centered 17sp title.
     - `SearchScreen` must use a standardized 44dp iOS Search Bar with `IosNavTextButton(text = "取消")`.

2. **Spring Touch Physics & Material Purge**:
   - *Observation*:
     - `CalendarScreen.kt:287` uses Material 3 `Button(...) { Text("写这天的日记") }`.
     - `CalendarScreen.kt:210` uses `iosClick(pressedScale = 0.90f)`.
     - `OnThisDayScreen.kt:59` uses Material 3 `IconButton`.
     - `SearchScreen.kt:139` and `TrashScreen.kt:79` use ad-hoc `Box` buttons.
   - *Inference*: Material components introduce ink ripple effects and inconsistent touch physics that contradict Apple HIG tactile standards.
   - *Conclusion*:
     - Replace Material 3 `Button` in `CalendarScreen` with an iOS Capsule Button applying `Modifier.iosClick` (scale 0.97f, alpha 0.85f).
     - Replace `pressedScale = 0.90f` in day cells with standard `0.97f`.
     - Replace Material 3 `IconButton` in `OnThisDayScreen` with `IosNavBackButton`.
     - Standardize text buttons to `IosNavTextButton`.

3. **Destructive Action Confirmation Safety**:
   - *Observation*: `TrashScreen.kt:179` calls `viewModel.permanentDelete(diary.id)` immediately upon tapping the delete icon.
   - *Inference*: HIG requires an alert confirmation before irreversible data loss.
   - *Conclusion*: Implement an `IosModalDialog` for individual item deletion, matching the existing batch empty trash confirmation dialog.

---

## 3. Caveats

- **CalendarScreen Scrolling Strategy**: `CalendarScreen` contains an interactive 7-column calendar grid above a diary list. If the entire screen is wrapped in a unified `LazyColumn` under `IosLargeTitleScaffold`, selecting days while scrolled down requires careful state retention. Therefore, the recommended blueprint preserves the pinned calendar view while standardizing the top bar to 44dp HIG navigation bar with `IosNavBackButton`, or implements a unified LazyColumn where DayGrid is rendered as chunked rows. Both blueprints are provided below.
- **Root Tab vs Pushed Screen Insets**: In `AppNavigation.kt`, `CalendarScreen` and `OnThisDayScreen` can act as root tabs (where bottom `IosTabBar` is visible and `onNavigateBack` is null) or as pushed modal screens. The bottom padding must dynamically adapt: `if (onNavigateBack != null) 32.dp else 96.dp`.
- **Domain Layer Protection**: ViewModels (`CalendarViewModel`, `OnThisDayViewModel`, `SearchViewModel`, `StatsViewModel`, `TrashViewModel`) and Room DAOs must remain completely untouched.

---

## 4. Conclusion & Implementation Blueprints

### Blueprint 1: `CalendarScreen.kt`

#### Changes:
1. Replace ad-hoc 52dp `Surface` with standard 44dp iOS Navigation Bar:
   - Bar height: `IosLargeTitleDefaults.TopBarHeight` (44.dp).
   - Hairline separator: `AppleMaterials.separatorColor(isDark)`.
   - Back button: `IosNavBackButton(onNavigateBack = onNavigateBack, label = "返回")` when `onNavigateBack != null`.
   - Centered title: 17sp SemiBold "日历", `SansFontFamily`.
2. Standardize day cell spring physics:
   - Change `.iosClick(pressedScale = 0.90f)` to `.iosClick { viewModel.selectDate(dayNum) }` (scale 0.97f, alpha 0.85f).
3. Purge Material 3 `Button` from empty state:
   - Replace with iOS Capsule Button using `Surface` + `CapsuleShape` + `Modifier.iosClick`.
4. Standardize bottom padding:
   - Use `bottom = if (onNavigateBack != null) 24.dp else 96.dp`.

#### Code Snippet Blueprint:
```kotlin
// 1. Top Bar Replacement
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .background(AppleMaterials.barBackgroundColor(isDark)),
    color = AppleMaterials.barBackgroundColor(isDark),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp
) {
    Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IosLargeTitleDefaults.TopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            if (onNavigateBack != null) {
                IosNavBackButton(
                    onNavigateBack = onNavigateBack,
                    label = "返回",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            Text(
                text = "日历",
                fontSize = 17.sp,
                fontFamily = SansFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppleMaterials.separatorColor(isDark)
        )
    }
}

// 2. Day Cell Scale Standardized
Box(
    modifier = Modifier
        .aspectRatio(1f)
        .clip(cellShape)
        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
        .iosClick { viewModel.selectDate(dayNum) },
    contentAlignment = Alignment.Center
)

// 3. Empty State Button Purge
Surface(
    modifier = Modifier
        .height(44.dp)
        .clip(CapsuleShape)
        .iosClick { onNavigateToEditor(null, uiState.selectedDate.timeInMillis) },
    shape = CapsuleShape,
    color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "写这天的日记",
            fontFamily = SansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
```

---

### Blueprint 2: `OnThisDayScreen.kt`

#### Changes:
1. Eliminate Material 3 `TopAppBar` and `IconButton`.
2. Migrate to `IosLargeTitleScaffold(title = "那年今日", lazyListState = listState, navigationIcon = ...)`.
3. Add `IosLargeTitleItem(title = "那年今日", subtitle = todayStr, scrollOffset = scrollOffset)` as the first item in `LazyColumn`.
4. Ensure `LazyColumn` contentPadding bottom is `if (onNavigateBack != null) 32.dp else 96.dp`.
5. Enforce `SansFontFamily` across all text elements.

#### Code Snippet Blueprint:
```kotlin
@Composable
fun OnThisDayScreen(
    viewModel: OnThisDayViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToEditor: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayFormatter = remember { SimpleDateFormat("M月d日", Locale.CHINESE) }
    val todayStr = remember { todayFormatter.format(Date()) }

    val listState = rememberLazyListState()
    val scrollOffset = rememberLazyListScrollOffset(listState)

    IosLargeTitleScaffold(
        title = "那年今日",
        lazyListState = listState,
        navigationIcon = if (onNavigateBack != null) {
            {
                IosNavBackButton(
                    onNavigateBack = onNavigateBack,
                    label = "返回"
                )
            }
        } else null
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        } else if (uiState.memories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                    .padding(horizontal = 16.dp)
            ) {
                IosLargeTitleItem(
                    title = "那年今日",
                    subtitle = todayStr,
                    scrollOffset = 0f
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "今天暂无往年回忆",
                        fontSize = 17.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "坚持每天写日记，未来的今天将重逢当下的自己",
                        fontSize = 13.sp,
                        fontFamily = SansFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (onNavigateBack != null) 32.dp else 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "large_title_header") {
                    IosLargeTitleItem(
                        title = "那年今日",
                        subtitle = todayStr,
                        scrollOffset = scrollOffset
                    )
                }

                items(uiState.memories, key = { it.diary.id }) { memory ->
                    // Memory PaperCard item with iosClick
                    ...
                }
            }
        }
    }
}
```

---

### Blueprint 3: `SearchScreen.kt`

#### Changes:
1. Standardize top bar to iOS Navigation Bar height (44dp + status bar insets):
   - Replace ad-hoc 56dp layout.
   - Use `IosNavTextButton(text = "取消", onClick = onNavigateBack)` for the cancel action.
2. Standardize search input capsule:
   - Height: 36dp with `RoundedCornerShape(10.dp)`.
   - Subtle translucent background: `if (isDark) Color(0x26FFFFFF) else Color(0x0F000000)`.
   - Hairline border: 0.5dp `AppleMaterials.separatorColor(isDark)`.
   - Clear icon button: 20dp circle with `iosIconClick`.
3. Typography:
   - Ensure all search result titles, previews, and dates specify `fontFamily = SansFontFamily`.

#### Code Snippet Blueprint:
```kotlin
// In topBar of SearchScreen:
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .background(AppleMaterials.barBackgroundColor(isDark)),
    color = AppleMaterials.barBackgroundColor(isDark),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp
) {
    Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IosLargeTitleDefaults.TopBarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Capsule
            BasicTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = SansFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                color = if (isDark) Color(0x24FFFFFF) else Color(0x0D000000),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = AppleMaterials.separatorColor(isDark),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = PaperColors.MonoGray500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = "搜索日记标题、内容、地点…",
                                    fontFamily = SansFontFamily,
                                    fontSize = 14.sp,
                                    color = PaperColors.MonoGray400
                                )
                            }
                            innerTextField()
                        }
                        if (uiState.query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(PaperColors.MonoGray400.copy(alpha = 0.3f))
                                    .iosIconClick { viewModel.updateQuery("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "清除",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Standardized iOS Cancel Action
            IosNavTextButton(
                text = "取消",
                onClick = onNavigateBack
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppleMaterials.separatorColor(isDark)
        )
    }
}
```

---

### Blueprint 4: `StatsScreen.kt`

#### Changes:
1. Replace ad-hoc 52dp `Surface` with `IosLargeTitleScaffold`:
   - `title = "数据与统计"`
   - `scrollState = scrollState`
   - `navigationIcon = { IosNavBackButton(onNavigateBack = onNavigateBack, label = "返回") }`
2. Insert `IosLargeTitleItem(title = "数据与统计", scrollOffset = scrollOffset)` at top of Column.
3. Content Column padding: `top = innerPadding.calculateTopPadding() + 8.dp`, `bottom = 32.dp`.
4. Typography: Enforce `fontFamily = SansFontFamily` on `StatNumberCard` and all text blocks.

#### Code Snippet Blueprint:
```kotlin
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()
    val scrollOffset = rememberScrollStateOffset(scrollState)

    IosLargeTitleScaffold(
        title = "数据与统计",
        scrollState = scrollState,
        navigationIcon = {
            IosNavBackButton(
                onNavigateBack = onNavigateBack,
                label = "返回"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 32.dp
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IosLargeTitleItem(
                title = "数据与统计",
                scrollOffset = scrollOffset,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 1. Core Metrics (StatNumberCard)
            ...
            // 2. Mood Distribution (PaperCard)
            ...
            // 3. Top Tags (PaperCard)
            ...
        }
    }
}
```

---

### Blueprint 5: `TrashScreen.kt`

#### Changes:
1. Replace ad-hoc 52dp `Surface` with `IosLargeTitleScaffold`:
   - `title = "回收站"`
   - `lazyListState = listState`
   - `navigationIcon = { IosNavBackButton(onNavigateBack = onNavigateBack, label = "返回") }`
   - `actions = { if (diaries.isNotEmpty()) { IosNavTextButton(text = "清空", color = MaterialTheme.colorScheme.error, isPrimary = true, onClick = { showEmptyConfirm = true }) } }`
2. Add `IosLargeTitleItem(title = "回收站", subtitle = if (diaries.isNotEmpty()) "${diaries.size} 篇已删除" else null, scrollOffset = scrollOffset)` in `LazyColumn`.
3. Add single diary permanent delete safety confirmation dialog (`IosModalDialog`):
   - State: `var diaryToDeletePermanently by remember { mutableStateOf<Diary?>(null) }`
   - Prompt user before permanent deletion.
4. Enforce `SansFontFamily` and 0.5dp separators.

#### Code Snippet Blueprint:
```kotlin
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onNavigateBack: () -> Unit
) {
    val diaries by viewModel.trashDiaries.collectAsState()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var diaryToDeletePermanently by remember { mutableStateOf<Diary?>(null) }

    val listState = rememberLazyListState()
    val scrollOffset = rememberLazyListScrollOffset(listState)

    IosLargeTitleScaffold(
        title = "回收站",
        lazyListState = listState,
        navigationIcon = {
            IosNavBackButton(
                onNavigateBack = onNavigateBack,
                label = "返回"
            )
        },
        actions = {
            if (diaries.isNotEmpty()) {
                IosNavTextButton(
                    text = "清空",
                    color = MaterialTheme.colorScheme.error,
                    isPrimary = true,
                    onClick = { showEmptyConfirm = true }
                )
            }
        }
    ) { innerPadding ->
        if (diaries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                    .padding(horizontal = 16.dp)
            ) {
                IosLargeTitleItem(
                    title = "回收站",
                    scrollOffset = 0f
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "回收站为空",
                        fontSize = 17.sp,
                        fontFamily = SansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "已删除的日记将在这里暂存",
                        fontSize = 13.sp,
                        fontFamily = SansFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "large_title_header") {
                    IosLargeTitleItem(
                        title = "回收站",
                        subtitle = "${diaries.size} 篇已删除",
                        scrollOffset = scrollOffset
                    )
                }

                items(diaries, key = { it.id }) { diary ->
                    val deletedTimeStr = remember(diary.deletedAt) {
                        if (diary.deletedAt != null) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(diary.deletedAt))
                        } else ""
                    }

                    PaperCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "删除于 $deletedTimeStr",
                                    fontSize = 12.sp,
                                    fontFamily = SansFontFamily,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .iosIconClick { viewModel.restore(diary.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Restore,
                                            contentDescription = "恢复",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .iosIconClick { diaryToDeletePermanently = diary },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteForever,
                                            contentDescription = "永久删除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (diary.title.isNotBlank()) {
                                Text(
                                    text = diary.title,
                                    fontSize = 17.sp,
                                    fontFamily = SansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = diary.previewText,
                                fontSize = 14.sp,
                                fontFamily = SansFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Batch Empty Confirmation Dialog
    IosModalDialog(
        visible = showEmptyConfirm,
        title = "清空回收站",
        message = "清空后所有已删除日记将被永久销毁，无法恢复。",
        confirmText = "清空",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            viewModel.emptyTrash()
            showEmptyConfirm = false
        },
        onDismissRequest = { showEmptyConfirm = false }
    )

    // 2. Single Item Permanent Delete Safety Confirmation Dialog
    IosModalDialog(
        visible = diaryToDeletePermanently != null,
        title = "永久删除日记",
        message = "确定要永久删除此日记吗？此操作无法撤销。",
        confirmText = "删除",
        cancelText = "取消",
        isDestructive = true,
        onConfirm = {
            diaryToDeletePermanently?.let { viewModel.permanentDelete(it.id) }
            diaryToDeletePermanently = null
        },
        onDismissRequest = { diaryToDeletePermanently = null }
    )
}
```

---

## 5. Verification Method

### 5.1 Verification Commands
1. **Unit Test Suite & Static Code Compilation**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   *Expected Output*: `BUILD SUCCESSFUL`, 0 errors, 100% test pass.
2. **Full Debug APK Build**:
   ```bash
   ./gradlew assembleDebug
   ```
   *Expected Output*: `BUILD SUCCESSFUL` with 0 compilation errors.
3. **Automated Material Purge Invariant Verification**:
   ```bash
   ./gradlew test --tests com.example.inkpaperdiary.tier1_features.MaterialIdiomPurgeAuditTest
   ```
   *Expected Output*: All purge invariant tests pass.

### 5.2 Files to Inspect Post-Implementation
- `app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/onthisday/OnThisDayScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/search/SearchScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/stats/StatsScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/trash/TrashScreen.kt`

### 5.3 Invalidation Conditions
- Any occurrence of `TopAppBar`, `IconButton`, `Button`, or `FloatingActionButton` remaining in secondary screens.
- Any clickable element lacking `Modifier.iosClick` or relying on Android Material ink ripple.
- Absence of `IosNavBackButton(label = "返回")` on pushed screens.
- Accidental regressions to Room DAOs, ViewModels, or sync pipelines.
