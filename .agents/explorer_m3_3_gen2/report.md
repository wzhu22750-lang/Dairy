# Forensic Investigation Report: Contextual Action Sheet & Interaction Architecture (Milestone 3)

**Agent**: Explorer M3-3 (Gen 2)  
**Target Module**: `TimelineScreen.kt`, `IosActionSheet.kt`, `TimelineViewModel.kt`, `AppNavigation.kt`  
**Date**: 2026-09-06  

---

## 1. Executive Summary

This investigation establishes the interaction blueprint and technical contract for the **Contextual Action Sheet** (`IosActionSheet`), card gesture handling (`onLongClick`), non-UI business logic preservation (`togglePin`, `moveToTrash`, Room DAOs), and the backward-compatibility fix in `AppNavigation.kt`.

### Core Conclusions:
1. **Contextual Action Sheet & Long-Press**: `PaperCard` and `Modifier.iosClick` already provide seamless `onLongClick` integration with spring compression (0.97f scale, 0.85f alpha) and tactile haptic feedback (`HapticFeedbackType.LongPress`). Long-pressing any diary card cleanly triggers `IosActionSheet` with dynamic pin/unpin toggling, direct editor navigation, destructive red "移入回收站" (Move to Trash), and a detached 14dp squircle "取消" (Cancel) pill.
2. **Absolute Idiom Eradication**: Both `Icons.Default.MoreVert` (3-dot overflow menu) and `DropdownMenu` have zero occurrences in `TimelineScreen.kt` and primary navigation. All diary entry actions are fully transferred to the native iOS bottom action sheet.
3. **100% Non-UI Domain Integrity**: Room DAO methods (`updatePinned`, `softDelete`), sync status transitions (`SyncStatus.DIRTY = 1`, `SyncStatus.DELETED = 2`), and reactive Flow emissions in `DiaryRepository` and `TimelineViewModel` remain completely intact and untouched.
4. **AppNavigation Backwards Compatibility**: 5 occurrences of `modalStack.removeLast()` in `AppNavigation.kt` must be converted to `modalStack.removeAt(modalStack.size - 1)` to eliminate potential `java.lang.NoSuchMethodError` on Android 14 and earlier (API < 35).

---

## 2. Technical Investigation & Codebase Analysis

### 2.1 Component Anatomy: `IosActionSheet.kt`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosActionSheet.kt`
- **Architecture**:
  - Encapsulates `ModalBottomSheet` with transparent container and 40% dim scrim.
  - Group card: 14dp squircle `RoundedCornerShape`, `AppleMaterials.backgroundColor(MaterialThickness.THICK)`, and 0.5dp specular hairline border (`AppleMaterials.glassBorder(width = 0.5.dp)`).
  - Optional Header: Centered 13sp SemiBold title + 12sp subtitle/date with 0.5dp hairline divider.
  - Row items: 56dp height, centered text (17sp), optional 20dp icon, spring compression (`Modifier.iosClick`), and 0.5dp separators between options.
  - Destructive styling: When `isDestructive = true`, tint is set to Apple Red (`Color(0xFFFF3B30)`), with standard font weight.
  - Detached Cancel pill: Separate bottom `Surface` (56dp height, 14dp squircle, glass border, 8dp top margin), bold 17sp font in primary system blue (`MaterialTheme.colorScheme.primary`).
  - Dismissal lifecycle: `onDismissRequest()` is invoked immediately on action click before firing the callback, guaranteeing clean sheet teardown and preventing re-entrancy bugs.

### 2.2 Gesture Physics: `PaperCard.kt` & `IosTouchPhysics.kt`
- **Location**: `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/PaperCard.kt` and `interaction/IosTouchPhysics.kt`
- **Gesture Pipeline**:
  ```kotlin
  // PaperCard.kt lines 50-58
  val interactionModifier = if (onClick != null) {
      Modifier.iosClick(
          enabled = enabled,
          onClick = onClick,
          onLongClick = onLongClick
      )
  } else {
      Modifier
  }
  ```
  ```kotlin
  // IosTouchPhysics.kt lines 118-141
  .pointerInput(enabled) {
      if (!enabled) return@pointerInput
      detectTapGestures(
          onPress = {
              isPressed = true
              if (currentHaptic) {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Tick on press down
              }
              val released = tryAwaitRelease()
              isPressed = false
          },
          onLongPress = {
              if (currentOnLongClick != null) {
                  if (currentHaptic) {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress) // Heavy haptic on hold
                  }
                  currentOnLongClick?.invoke()
              }
          },
          onTap = {
              currentOnClick()
          }
      )
  }
  ```
- **Accessibility & Semantics**:
  `Modifier.iosClick` attaches `Role.Button`, `onClick`, and `onLongClick` semantics, ensuring TalkBack screen readers announce: "双击以激活，点按并按住以长按".

---

## 3. Contextual Action Sheet Specification for Timeline

### 3.1 State Management in `TimelineContent`
```kotlin
// State hoisted in TimelineContent composable
var selectedDiaryForAction by remember { mutableStateOf<Diary?>(null) }
```

### 3.2 Card Gesture Wiring in `LazyColumn`
```kotlin
items(uiState.filteredDiaries, key = { it.id }) { diary ->
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        DiaryCardItem(
            diary = diary,
            onClick = { onNavigateToEditor(diary.id) },
            onLongClick = { selectedDiaryForAction = diary }
        )
    }
}
```

### 3.3 Contextual Action Sheet Rendering
```kotlin
val actionDiary = selectedDiaryForAction
if (actionDiary != null) {
    val isPinned = actionDiary.isPinned
    val displayTitle = if (actionDiary.title.isNotBlank()) {
        if (actionDiary.title.length > 28) actionDiary.title.take(28) + "..." else actionDiary.title
    } else {
        val snippet = actionDiary.previewText.take(28)
        if (snippet.isNotBlank()) "$snippet..." else "日记操作"
    }
    val dateStr = remember(actionDiary.entryDate) {
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINESE).format(Date(actionDiary.entryDate))
    }

    IosActionSheet(
        visible = true,
        title = displayTitle,
        message = dateStr,
        actions = listOf(
            IosActionItem(
                title = if (isPinned) "取消置顶" else "置顶此篇",
                icon = if (isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
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
        cancelText = "取消",
        onDismissRequest = { selectedDiaryForAction = null }
    )
}
```

---

## 4. Non-UI Business Logic & Data Flow Protection

### 4.1 Pin / Unpin Data Flow (`togglePin`)
1. Action item clicked: `onClick = { onTogglePin(actionDiary) }`
2. `TimelineViewModel.togglePin(diary)` launches a coroutine on `viewModelScope`:
   ```kotlin
   fun togglePin(diary: Diary) {
       viewModelScope.launch {
           repository.togglePin(diary.id)
       }
   }
   ```
3. `DiaryRepository.togglePin(id)`:
   - Queries `diaryDao.getDiaryEntityAnyStatus(id)`
   - Calls `diaryDao.updatePinned(id, !current.isPinned)`
4. `DiaryDao.updatePinned(id, pinned)`:
   ```sql
   UPDATE diary_entries SET isPinned = :pinned, syncStatus = 1 WHERE id = :id
   ```
   - Sets `isPinned` to opposite boolean.
   - Sets `syncStatus = 1` (`SyncStatus.DIRTY`), ensuring upcoming Supabase cloud sync automatically pushes this modification.
   - Triggers Room invalidation tracker on `diary_entries` table.
5. Room re-emits through `diaryDao.getAllDiaries()` (`ORDER BY isPinned DESC, entryDate DESC`), seamlessly floating the pinned diary to the top of the timeline or re-ordering without manual list splicing in the UI.

### 4.2 Move to Trash Data Flow (`deleteDiary`)
1. Destructive action clicked: `onClick = { onDeleteDiary(actionDiary.id) }`
2. `TimelineViewModel.deleteDiary(diaryId)` launches a coroutine on `viewModelScope`:
   ```kotlin
   fun deleteDiary(diaryId: String) {
       viewModelScope.launch {
           repository.softDeleteDiary(diaryId)
       }
   }
   ```
3. `DiaryRepository.softDeleteDiary(id)` calls `diaryDao.softDelete(id, System.currentTimeMillis())`.
4. `DiaryDao.softDelete(id, deletedAt)`:
   ```sql
   UPDATE diary_entries SET isDeleted = 1, deletedAt = :deletedAt, syncStatus = 2 WHERE id = :id
   ```
   - Sets `isDeleted = 1`, `deletedAt = timestamp`, and `syncStatus = 2` (`SyncStatus.DELETED`).
   - Does NOT delete attachments or database records (soft delete).
5. Room re-emits through `getAllDiaries()` (`WHERE isDeleted = 0`), immediately removing the diary from the active timeline stream.
6. The soft-deleted diary safely resides in `getTrashDiaries()` (`WHERE isDeleted = 1`), allowing full restoration or permanent purge from `TrashScreen`.

---

## 5. Elimination of Android Material Idioms

| Prohibited Android Idiom | Replacement in Apple HIG Architecture | Verification Status |
|---|---|---|
| `Icons.Default.MoreVert` (3-dot menu button) | Long-press card gesture -> `IosActionSheet` | **0 occurrences** in `TimelineScreen.kt` |
| `DropdownMenu` / `DropdownMenuItem` | `IosActionSheet` with grouped squircle card and detached cancel pill | **0 occurrences** in `TimelineScreen.kt` |
| `FloatingActionButton` (FAB) | Top-right trailing navigation bar compose icon (`Icons.Outlined.Edit`) | **0 occurrences** in `TimelineScreen.kt` |
| Ink Ripple effects | `Modifier.iosClick` spring scale (0.97f) + alpha (0.85f) + tactile haptics | Complete across all cards & sheet rows |

---

## 6. Backward Compatibility Fix: `AppNavigation.kt`

### 6.1 Issue Diagnosis
In Kotlin standard library, `removeLast()` is an extension function on `MutableList`. However, in Java 21 / Android SDK 35 (`compileSdk = 36`), `java.util.List` introduces a native `removeLast()` method.

When compiled against Java 17/21 bytecode targeting Android:
- Calls to `modalStack.removeLast()` on a `SnapshotStateList` can resolve to `INVOKEINTERFACE java/util/List.removeLast ()Ljava/lang/Object;`.
- On Android runtime (ART) on devices running Android 8.0 through Android 14 (API 26–34), `java.util.List` does not possess `removeLast()`.
- This can result in a fatal runtime crash: `java.lang.NoSuchMethodError: No interface method removeLast()Ljava/lang/Object; in class Ljava/util/List;`.

### 6.2 Solution
Replace all occurrences of `modalStack.removeLast()` with `modalStack.removeAt(modalStack.size - 1)`.
The method `removeAt(Int)` maps to `java.util.List.remove(int)`, which is universally supported across all Android API versions (API 1+).

### 6.3 Exact Code Locations in `AppNavigation.kt`

#### Location 1: BackHandler (Line 77)
```kotlin
// BEFORE
BackHandler(
    enabled = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)
) {
    if (modalStack.isNotEmpty()) {
        if (currentModal !is AppDestination.Editor) {
            modalStack.removeLast()
        }
    } else if (selectedTab != IosTab.JOURNAL) {
        selectedTab = IosTab.JOURNAL
    }
}

// AFTER
BackHandler(
    enabled = !(isLockEnabled && isAppLocked) && (modalStack.isNotEmpty() || selectedTab != IosTab.JOURNAL)
) {
    if (modalStack.isNotEmpty()) {
        if (currentModal !is AppDestination.Editor) {
            modalStack.removeAt(modalStack.size - 1)
        }
    } else if (selectedTab != IosTab.JOURNAL) {
        selectedTab = IosTab.JOURNAL
    }
}
```

#### Location 2: EditorScreen `onNavigateBack` (Line 115)
```kotlin
// BEFORE
EditorScreen(
    viewModel = editorViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeLast()
    }
)

// AFTER
EditorScreen(
    viewModel = editorViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
    }
)
```

#### Location 3: SearchScreen `onNavigateBack` (Line 124)
```kotlin
// BEFORE
SearchScreen(
    viewModel = searchViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeLast()
    },
...

// AFTER
SearchScreen(
    viewModel = searchViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
    },
...
```

#### Location 4: StatsScreen `onNavigateBack` (Line 136)
```kotlin
// BEFORE
StatsScreen(
    viewModel = statsViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeLast()
    }
)

// AFTER
StatsScreen(
    viewModel = statsViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
    }
)
```

#### Location 5: TrashScreen `onNavigateBack` (Line 145)
```kotlin
// BEFORE
TrashScreen(
    viewModel = trashViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeLast()
    }
)

// AFTER
TrashScreen(
    viewModel = trashViewModel,
    onNavigateBack = {
        if (modalStack.isNotEmpty()) modalStack.removeAt(modalStack.size - 1)
    }
)
```

---

## 7. Drop-in Implementation Blueprint for Worker M3

### File 1: `TimelineScreen.kt` Action Sheet Integration
In `TimelineContent`:
1. Ensure parameter list retains:
   - `onTogglePin: (Diary) -> Unit`
   - `onDeleteDiary: (String) -> Unit`
   - `onNavigateToEditor: (String?) -> Unit`
2. State declaration:
   ```kotlin
   var selectedDiaryForAction by remember { mutableStateOf<Diary?>(null) }
   ```
3. Pass `onLongClick = { selectedDiaryForAction = diary }` into `DiaryCardItem`.
4. Render `IosActionSheet` unconditionally based on `selectedDiaryForAction != null`.

### File 2: `AppNavigation.kt` Safe Pop Modification
Apply the 5 line replacements from Section 6.3.

---

## 8. Verification Strategy & Test Cases

Worker M3 and Challenger agents can verify this implementation using:

1. **Idiom Eradication Static Audit**:
   ```bash
   grep -rn "Icons.Default.MoreVert" app/src/main/
   grep -rn "DropdownMenu" app/src/main/
   grep -rn "FloatingActionButton" app/src/main/
   ```
   Must yield 0 lines of output.

2. **Compatibility Audit**:
   ```bash
   grep -rn "removeLast" app/src/main/java/com/example/inkpaperdiary/ui/navigation/
   ```
   Must yield 0 lines of output.

3. **Room Reactive Invalidation Test**:
   - Create a test diary entity with `isPinned = false`.
   - Invoke `repository.togglePin(id)`.
   - Assert `getAllDiaries()` emits list where first diary has `isPinned == true` and `syncStatus == 1`.
   - Invoke `repository.softDeleteDiary(id)`.
   - Assert `getAllDiaries()` emits empty list, and `getTrashDiaries()` emits list with `isDeleted == true` and `syncStatus == 2`.

4. **Compilation & Build**:
   ```bash
   ./gradlew :app:testDebugUnitTest --tests "com.example.inkpaperdiary.*"
   ./gradlew assembleDebug
   ```
   Must pass with 0 errors.
