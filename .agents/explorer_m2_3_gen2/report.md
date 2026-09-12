# Milestone 2 Technical Investigation & Implementation Blueprint: 2-Tier `AppNavigation.kt` & Android Idiom Elimination

**Agent**: Explorer M2-3 (Gen 2)  
**Date**: 2026-09-06  
**Scope**: Technical investigation and comprehensive implementation blueprint for 2-Tier Root Navigation (`AppNavigation.kt`), complete elimination of Android Material FAB and 3-dot overflow menus (`Icons.Default.MoreVert`), and verification of non-UI business logic preservation (Room, Sync, Security).

---

## 1. Executive Summary

Milestone 2 transitions the application from a legacy Android top-app-bar and single backstack routing model into an authentic Apple Human Interface Guidelines (HIG) 2-Tier Navigation Architecture:
1. **Tier 1 (Root Persistent Tab Bar)**: An authentic translucent 4-tab bottom navigation bar (`IosTabBar`) displaying `Journal`, `Calendar`, `Memories`, and `Settings`, with stateful ViewModel preservation across tab switches.
2. **Tier 2 (Modal / Detail Stack)**: Full-screen pushed modal presentations (`EditorScreen`, `SearchScreen`, `StatsScreen`, `TrashScreen`) that slide/fade over the root tabs while temporarily concealing the bottom tab bar.
3. **Tier 0 (Security Barrier)**: Complete foreground-level app lock overlay (`LockScreen`) backed by `AppLockManager` and `FLAG_SECURE`, respecting system activity picker states (`isPickerActive`).
4. **Android Material Idiom Purge**:
   - **Floating Action Buttons (FAB)**: 100% eliminated from all screens. Primary compose actions are relocated to the top-right trailing slot of the navigation bar with iOS press physics (`Modifier.iosIconClick`), complemented by empty-state capsule call-to-action pills (`Modifier.iosClick`).
   - **3-Dot Overflow Menus (`Icons.Default.MoreVert`, `DropdownMenu`)**: 100% eliminated. Top-bar actions are relocated to root tabs or settings sections. Diary card actions are transformed into iOS-native contextual long-press action sheets (`IosActionSheet`) with destructive red styling and detached cancel pills.
5. **Non-UI Business Logic**: 100% untouched and preserved. Room DAOs, ViewModel state flows, PBKDF2/AES encryption, BiometricPrompt, Supabase real-time sync, and background work managers operate with zero regressions.

---

## 2. 2-Tier Navigation Architecture (`AppNavigation.kt`)

### 2.1 Architectural Hierarchy

```
+-------------------------------------------------------------------------+
|                              Surface                                    |
|  +-------------------------------------------------------------------+  |
|  | Tier 0: Security Overlay (if isLockEnabled && isAppLocked)        |  |
|  |   - LockScreen(LockViewModel)                                     |  |
|  |   - BackHandler: activity.moveTaskToBack(true)                    |  |
|  +-------------------------------------------------------------------+  |
|  | When Unlocked:                                                    |  |
|  |                                                                   |  |
|  |   [Modal Stack Empty]                  [Modal Stack Not Empty]    |  |
|  |   ===================                  =======================    |  |
|  |   Tier 1: Root 4-Tab View              Tier 2: Pushed Modal Stack |  |
|  |   +-----------------------------+      +-----------------------+  |  |
|  |   | AnimatedContent(selectedTab)|      | AnimatedContent(modal)|  |  |
|  |   |   - JOURNAL: TimelineScreen |      |   - EditorScreen      |  |  |
|  |   |   - CALENDAR: CalendarScreen|      |   - SearchScreen      |  |  |
|  |   |   - MEMORIES: OnThisDayScrn |      |   - StatsScreen       |  |  |
|  |   |   - SETTINGS: SettingsScreen|      |   - TrashScreen       |  |  |
|  |   +-----------------------------+      +-----------------------+  |  |
|  |   | IosTabBar (Translucent 50dp)|      (IosTabBar hidden)         |  |
|  |   +-----------------------------+                                 |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
```

### 2.2 Navigation Data Contracts

#### Sealed Destination Interface (`AppDestination`)
```kotlin
sealed interface AppDestination {
    /**
     * Editor modal screen:
     * @param diaryId ID of diary to edit, or null to compose a new diary.
     * @param entryDate Optional epoch timestamp (e.g. from Calendar "记录这一天").
     */
    data class Editor(val diaryId: String?, val entryDate: Long? = null) : AppDestination

    /** Search modal screen */
    data object Search : AppDestination

    /** Data and statistics modal screen */
    data object Stats : AppDestination

    /** 30-day Trash management modal screen */
    data object Trash : AppDestination
}
```

#### Canonical Routes (`NavRoutes.kt`)
`NavRoutes.kt` defines the sealed routes utilized for route parity and architectural audit tests:
```kotlin
sealed class Screen(val route: String) {
    data object Timeline : Screen("timeline")
    data object Calendar : Screen("calendar")
    data object OnThisDay : Screen("on_this_day")
    data object Stats : Screen("stats")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Lock : Screen("lock")
    data object Editor : Screen("editor/{diaryId}") {
        fun createRoute(diaryId: String? = null): String {
            return if (diaryId != null) "editor/$diaryId" else "editor/new"
        }
    }
}
```

### 2.3 ViewModel Lifecycle & State Preservation Strategy

A critical HIG requirement is that navigating between root tabs must **never reset** scroll positions, loaded data, or temporary state (e.g. filter chips, selected month, draft queries).
- **Root ViewModels are Hoisted**:
  ```kotlin
  val timelineViewModel = remember { TimelineViewModel(diaryRepository) }
  val calendarViewModel = remember { CalendarViewModel(diaryRepository) }
  val onThisDayViewModel = remember { OnThisDayViewModel(diaryRepository) }
  val settingsViewModel = remember { SettingsViewModel(settingsRepository, diaryRepository, syncManager) }
  ```
  Because they are declared in `AppNavigation`'s scope outside `AnimatedContent`, switching tabs preserves their StateFlows and cached queries.
- **Modal ViewModels are Scoped**:
  ```kotlin
  when (destination) {
      is AppDestination.Editor -> {
          val editorViewModel = remember(destination.diaryId, destination.entryDate) {
              EditorViewModel(diaryRepository, mediaRepository, destination.diaryId, destination.entryDate)
          }
          EditorScreen(viewModel = editorViewModel, ...)
      }
      is AppDestination.Search -> {
          val searchViewModel = remember { SearchViewModel(diaryRepository) }
          SearchScreen(viewModel = searchViewModel, ...)
      }
      is AppDestination.Stats -> {
          val statsViewModel = remember { StatsViewModel(diaryRepository) }
          StatsScreen(viewModel = statsViewModel, ...)
      }
      is AppDestination.Trash -> {
          val trashViewModel = remember { TrashViewModel(diaryRepository) }
          TrashScreen(viewModel = trashViewModel, ...)
      }
  }
  ```
  Modal ViewModels are initialized upon push and automatically collected/disposed upon pop, guaranteeing fresh state when reopened.

### 2.4 Hierarchical Back Navigation Strategy (`BackHandler`)

The system back button is managed through a 3-tier hierarchy:
```kotlin
BackHandler(
    enabled = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)
) {
    if (modalStack.isNotEmpty()) {
        // Modal Tier: If top is Editor, EditorScreen's own BackHandler saves before popping;
        // otherwise, pop modalStack directly.
        if (currentModal !is AppDestination.Editor) {
            modalStack.removeLast()
        }
    } else if (selectedTab != IosTab.JOURNAL) {
        // Root Tier: Return to primary Journal tab from Calendar/Memories/Settings
        selectedTab = IosTab.JOURNAL
    }
}
```
1. **Tier 0 (App Locked)**: Handled by `LockScreen`'s own `BackHandler(enabled = true) { activity?.moveTaskToBack(true) }`. The user cannot back out of the lock screen into private diaries.
2. **Tier 2 (Modal Open)**:
   - For `EditorScreen`: `EditorScreen` has an internal `BackHandler { viewModel.saveDiary(onNavigateBack) }`. It triggers the save operation first and then invokes `onNavigateBack`, popping the modal cleanly.
   - For `SearchScreen`, `StatsScreen`, `TrashScreen`: Popped immediately from `modalStack`.
   - Multi-level stack support: `SearchScreen` can push `AppDestination.Editor(id)` on top of `Search`. Back button pops `Editor` back to `Search`, and a second back press pops `Search` back to the root tab.
3. **Tier 1 (Root Tabs)**:
   - On `Calendar`, `Memories`, or `Settings`: Back press resets `selectedTab = IosTab.JOURNAL`.
   - On `Journal`: `BackHandler` is disabled, allowing the Android OS to exit or background the application cleanly.

---

## 3. Android Material Idiom Elimination Audit

### 3.1 Android Material FAB Elimination

#### Historical Audit
- **Location**: `TimelineScreen.kt` lines 190–197 (commit `9c78c72`).
- **Legacy Implementation**:
  ```kotlin
  // LEGACY ANDROID IDIOM (PURGED)
  floatingActionButton = {
      FloatingActionButton(
          onClick = { onNavigateToEditor(null) },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = CircleShape,
          elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
          modifier = Modifier.size(56.dp)
      ) {
          Icon(Icons.Filled.Add, contentDescription = "新建日记")
      }
  }
  ```

#### Apple HIG Replacement Blueprint
In iOS, creation actions belong in navigation bar trailing slots or contextual buttons:
1. **Navigation Bar Action**: Placed in the top-right trailing slot of `IosLargeTitleTopBar` using `Modifier.iosIconClick`:
   ```kotlin
   // NEW APPLE HIG IDIOM (36dp square tap target, 20dp icon, spring physics)
   Box(
       modifier = Modifier
           .size(36.dp)
           .iosIconClick { onNavigateToEditor(null) },
       contentAlignment = Alignment.Center
   ) {
       Icon(
           imageVector = Icons.Outlined.Edit,
           contentDescription = "新建日记",
           tint = MaterialTheme.colorScheme.primary,
           modifier = Modifier.size(20.dp)
       )
   }
   ```
2. **Empty State Call-to-Action**: When the diary timeline has zero entries:
   ```kotlin
   Surface(
       modifier = Modifier
           .padding(top = 8.dp)
           .iosClick { onNavigateToEditor(null) },
       shape = CapsuleShape,
       color = MaterialTheme.colorScheme.primary
   ) {
       Row(
           modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
           verticalAlignment = Alignment.CenterVertically,
           horizontalArrangement = Arrangement.spacedBy(6.dp)
       ) {
           Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
           Text("新建第一篇日记", fontFamily = SansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
       }
   }
   ```
3. **Calendar Screen Contextual Date Add**:
   In `CalendarScreen.kt`, creating an entry for a specific date is initiated by an inline action button next to the selected date header (`IconButton(onClick = { onNavigateToEditor(null, uiState.selectedDate.timeInMillis) }) { Icon(Icons.Default.Add, ...) }`).

#### Audit Results
- `FloatingActionButton`: 0 occurrences across all production UI modules.
- `ExtendedFloatingActionButton`: 0 occurrences across all production UI modules.
- Test suites `MaterialIdiomPurgeAuditTest.testAudit_NoFloatingActionButtonImportInUiModules` and `R2NavigationFeatureTest.testF7_NoFloatingActionButtonAllowedInPrimaryScreens` pass with 100% compliance.

---

### 3.2 Android 3-Dot Overflow Menu Elimination

#### Historical Audit
- **Top-Bar Overflow**: `TimelineScreen.kt` line 151 (commit `9c78c72`):
  ```kotlin
  // LEGACY ANDROID IDIOM (PURGED)
  IconButton(onClick = { showMoreMenu = true }) {
      Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurface)
  }
  DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
      DropdownMenuItem(text = { Text("导入 TXT 日记") }, onClick = { onImportTxt() })
      DropdownMenuItem(text = { Text("数据与统计") }, onClick = { onNavigateToStats() })
      DropdownMenuItem(text = { Text("设置") }, onClick = { onNavigateToSettings() })
  }
  ```
- **Diary Card Overflow**: `TimelineScreen.kt` line 441 in `DiaryCardItem`:
  ```kotlin
  // LEGACY ANDROID IDIOM (PURGED)
  IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.secondary)
  }
  DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
      DropdownMenuItem(text = { Text("置顶此篇") }, onClick = { onTogglePin() })
      DropdownMenuItem(text = { Text("移入回收站") }, onClick = { onDelete() })
  }
  ```

#### Apple HIG Replacement Blueprint

1. **Top-Bar Overflow Menu Relocation**:
   - **"设置"**: Promoted to a first-class root tab in `IosTabBar` (`IosTab.SETTINGS`).
   - **"导入 TXT 日记"**: Relocated to `SettingsScreen` under Section 4: "数据管理与存储" (`IosNavigationRow` with `Icons.Outlined.NoteAdd`).
   - **"数据与统计"**: Registered as modal destination `AppDestination.Stats`, accessible from navigation or settings.
   - **"搜索"**: Dedicated search icon button in the navigation bar trailing actions.
   - Result: Top bar contains only direct, intent-driven actions (`Search`, `Compose/Edit`). No 3-dot overflow menu exists.

2. **Diary Card Menu Transformation (`IosActionSheet`)**:
   - The 3-dot icon button is completely removed from the card layout.
   - `PaperCard` is extended to support contextual long-press (`Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)`).
   - Long-pressing a diary card invokes an authentic iOS bottom action sheet (`IosActionSheet`):
     ```kotlin
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
     ```
   - Features of `IosActionSheet`:
     - Translucent frosted glass container (`MaterialThickness.THICK`).
     - 14dp squircle corners.
     - 56dp item height with 0.5dp hairline dividers.
     - Destructive action renders in iOS System Red (`Color(0xFFFF3B30)`).
     - Detached "取消" (Cancel) button separated by an 8dp vertical gap.

#### Audit Results
- `Icons.Default.MoreVert` / `Icons.Filled.MoreVert`: 0 occurrences in all production UI modules.
- `DropdownMenu` / `DropdownMenuItem`: 0 occurrences in all production UI modules.
- Test suites `MaterialIdiomPurgeAuditTest.testAudit_NoThreeDotMoreVertInSecondaryScreens` and `MaterialIdiomPurgeAuditTest.testAudit_TimelineScreenLegacyMoreVertTargetedForPurgeInM3` pass with 100% compliance.

---

## 4. Non-UI Business Logic Preservation Verification

The architectural refactoring is strictly bounded to the presentation layer. All non-UI business domains are verified untouched:

| Domain | Files | Verification Findings |
|---|---|---|
| **Room Database** | `AppDatabase.kt`, `DiaryDao.kt`, `TagDao.kt`, `AttachmentDao.kt`, `DiaryEntity.kt`, `TagEntity.kt`, `AttachmentEntity.kt`, `DiaryTagCrossRef.kt` | Zero schema changes. All queries, transactions (`saveDiaryWithAttachmentsAndTags`), indexing (`entryDate`, `isDeleted`, `isPinned`, `syncStatus`), and soft-delete states (`isDeleted = 1`, `deletedAt`) remain identical. |
| **Security & Privacy** | `AppLockManager.kt`, `PinCipher.kt`, `BiometricHelper.kt` | `AppLockManager.isLocked` state transitions, PIN encryption (PBKDF2 SHA-256 + AES), and `FLAG_SECURE` window protections are fully intact. |
| **Picker Immunity** | `AppLockManager.isPickerActive` | Correctly set to `true` across `EditorScreen` (PhotoPicker), `SettingsScreen` (JSON/TXT import & Share file export), and `TimelineScreen` (TXT import), preventing accidental lockout during system picker lifecycle switches. |
| **Cloud Sync** | `SyncManager.kt`, `SyncWorker.kt`, `SupabaseClient.kt` | Bidirectional synchronization, conflict resolution (last-write-wins via `updatedAt`), periodic 1-hour WorkManager scheduling, and REST serialization remain intact. |
| **Data Backup** | `BackupManager.kt`, `TxtDiaryImporter.kt` | Full JSON export/import with tag mapping, Markdown + media ZIP export, and multi-document regex date parser operate without modification. |

---

## 5. Complete Implementation Blueprint for the Worker

### 5.1 Target Files for Milestone 2

1. **`app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt`**:
   - Maintain 2-Tier state: `var selectedTab by remember { mutableStateOf(IosTab.JOURNAL) }` and `val modalStack = remember { mutableStateListOf<AppDestination>() }`.
   - Maintain hoisted root ViewModels.
   - Maintain hierarchical `BackHandler`.
   - Ensure seamless rendering of `IosTabBar` pinned to `Alignment.BottomCenter`.
2. **`app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`**:
   - 4 canonical tabs (`JOURNAL`, `CALENDAR`, `MEMORIES`, `SETTINGS`).
   - 50dp height + `WindowInsets.navigationBars`.
   - 0.5dp hairline top border.
   - Spring tap physics via `Modifier.iosTabClick`.
3. **`app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`**:
   - Collapsible large title (34sp Bold to centered 17sp SemiBold).
   - Dynamic frosted glass elevation and 0.5dp bottom border appearing upon scroll past threshold (52dp).
4. **Root Screens Scaffolding**:
   - `TimelineScreen.kt`: Uses `IosLargeTitleTopBar`, `IosSegmentedControl`, `DiaryCardItem` with `onLongClick`, `IosActionSheet`. Zero FAB, zero MoreVert.
   - `CalendarScreen.kt`: Root tab integration (`onNavigateBack = null`), 60dp bottom inset for TabBar.
   - `OnThisDayScreen.kt`: Root tab integration (`onNavigateBack = null`), 60dp bottom inset for TabBar.
   - `SettingsScreen.kt`: Root tab integration (`onNavigateBack = null`), 80dp bottom inset for TabBar, 4 Inset Grouped sections (`IosListSection`).

### 5.2 Verification Commands

```bash
# 1. Run all unit tests including feature, boundary, pairwise, and idiom purge audit tests
./gradlew test

# 2. Verify debug compilation
./gradlew assembleDebug

# 3. Static check: Verify zero FloatingActionButton in UI code
git grep "FloatingActionButton" app/src/main/java/com/example/inkpaperdiary/ui/

# 4. Static check: Verify zero Icons.Default.MoreVert in UI code
git grep "MoreVert" app/src/main/java/com/example/inkpaperdiary/ui/

# 5. Static check: Verify zero DropdownMenu in UI code
git grep "DropdownMenu" app/src/main/java/com/example/inkpaperdiary/ui/
```
