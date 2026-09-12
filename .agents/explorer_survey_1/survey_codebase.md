# Android Jetpack Compose Codebase Survey & Apple HIG Refactoring Architecture Report

**Project**: `com.example.inkpaperdiary` (日记本 / Ink Paper Diary)  
**Survey Date**: 2026-09-06  
**Investigator**: Explorer 1  
**Project Root**: `/Users/kuangqie/Documents/VibeCoding/日记本`  
**Working Directory**: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_survey_1`

---

## Executive Summary

The existing application is a clean, modern Android Jetpack Compose application with an offline-first Room database, Supabase cloud synchronization, biometric/PIN app lock, and a rich diary management feature set (Markdown editing, photo attachments, mood/weather tagging, timeline, calendar, memories/on-this-day, stats, and 30-day trash recovery).

While recent styling efforts introduced early Apple HIG concepts (e.g., `AppleMaterials` translucency and `PaperTypography` SF-style typography), the application remains heavily entrenched in **Android and Material 3 idioms**:
1. Static Material 3 `TopAppBar` across every screen.
2. Floating Action Button (FAB) in `TimelineScreen`.
3. Material 3-dot overflow menus (`Icons.Default.MoreVert` + `DropdownMenu`) on both the timeline top bar and on each diary item card.
4. Standard Material ink ripple effects on all clicks (`PaperCard`, `Button`, `IconButton`, `FilterChip`).
5. Android framework modal pickers (`DatePickerDialog`, `TimePickerDialog`) and Material `AlertDialog` instances.
6. A single-stack flat navigation flow without the signature iOS **Bottom Translucent Tab Bar** and **Dynamic Collapsible Large Title**.

This survey provides an exhaustive inventory of the current UI components, maps every Material 3 idiom with exact file and line references, analyzes the navigation graph, and defines precise file boundaries for refactoring and new component creation to fulfill Requirements R1 through R4 with zero regression in business logic.

---

## 1. Architecture & Technology Stack Overview

### 1.1 Gradle & Platform Environment
- **Compile SDK**: 36 (Android 16 preview / Android 15+)
- **Target SDK**: 36, **Min SDK**: 26 (Android 8.0 Oreo)
- **Android Gradle Plugin**: 9.0.1
- **Kotlin**: 2.3.20 (JVM Toolchain 17)
- **Compose Compiler Plugin**: Jetpack Compose 2.3.20 / Compose BOM `2026.03.01`
- **KSP**: 2.3.11
- **Database**: AndroidX Room 2.7.2 with KSP code generation
- **Image Loading**: Coil Compose 2.7.0
- **Serialization & Network**: Kotlinx Serialization JSON 1.7.3, OkHttp 4.12.0
- **Background Work**: WorkManager 2.9.1 (`SyncWorker`)
- **Security & Preferences**: AndroidX Biometric 1.2.0-alpha05, DataStore Preferences 1.1.1

### 1.2 Compilation & Test Baseline
- Build command: `./gradlew assembleDebug` (Verified: BUILD SUCCESSFUL, 0 errors)
- Unit test command: `./gradlew test` (Verified: BUILD SUCCESSFUL, 26 tasks up-to-date, tests pass)
  - `DiaryModelTest`
  - `BackupManagerTest`
  - `TxtDiaryImporterTest`

---

## 2. Existing UI Architecture Inventory

### 2.1 Screens & Composables Matrix

| Screen / Feature | Primary Composable | File Path | Sub-Composables & Dialogs |
| :--- | :--- | :--- | :--- |
| **Timeline** | `TimelineScreen` | `ui/timeline/TimelineScreen.kt` | `TimelineContent`, `DiaryCardItem`, Filter LazyRow, EmptyState |
| **Editor** | `EditorScreen` | `ui/editor/EditorScreen.kt` | Date/Time Picker Trigger, Markdown BottomBar LazyRow, Mood/Weather Selector, Tag/Location Row, Image Preview LazyRow, Tag Dialog, Location Dialog |
| **Calendar** | `CalendarScreen` | `ui/calendar/CalendarScreen.kt` | Month Navigation Bar, Week Header, Monthly Grid (`LazyVerticalGrid`), Selected Day Diary List |
| **On This Day (Memories)** | `OnThisDayScreen` | `ui/onthisday/OnThisDayScreen.kt` | Memory List (`LazyColumn`), Memory Diary Card |
| **Search** | `SearchScreen` | `ui/search/SearchScreen.kt` | Search Top Bar (`BasicTextField`), Mood Filter Row, Weather Filter Row, Tag Filter Row, Search Results List |
| **Settings** | `SettingsScreen` | `ui/settings/SettingsScreen.kt` | Supabase Config Section, Sync Section, App Lock Section, Paper Texture Section, Data Management Section, Supabase Dialog, PIN Dialog |
| **Data & Stats** | `StatsScreen` | `ui/stats/StatsScreen.kt` | `StatNumberCard` (Streak, Total, Words), Mood Distribution Bar Chart, Top Tags Cloud |
| **Trash** | `TrashScreen` | `ui/trash/TrashScreen.kt` | Trash List (`LazyColumn`), Trash Card with Restore / Permanent Delete, Empty Trash Confirm Dialog |
| **Lock Screen** | `LockScreen` | `ui/lock/LockScreen.kt` | Lock Icon Header, 6-Dot PIN Indicator, 3x4 Circular Keypad Grid (`LazyVerticalGrid`), Biometric Trigger |

### 2.2 Reusable Design System Components

| Component | File Path | Description & Current State |
| :--- | :--- | :--- |
| `AppleMaterials` | `core/designsystem/AppleMaterial.kt` | Defines `MaterialThickness` (5 levels), `AppleMaterials.backgroundColor`, `glassBorder` (0.5dp gradient), `VibrancyLevel`, and `Modifier.appleMaterial`. |
| `PaperCard` | `core/designsystem/components/PaperCard.kt` | 16dp rounded card with `AppleMaterials.glassBorder` and optional accent stripe. **Issue**: Uses `Modifier.clickable` with standard Material ink ripple. |
| `PaperIcons` | `core/designsystem/components/PaperIcons.kt` | Custom canvas hand-drawn vector glyphs (`MoodIcon`, `WeatherIcon`, `FlameGlyph`, `ScrollGlyph`, `QuillGlyph`). Self-contained, zero Android/Material dependency. |
| `RuledBackground` | `core/designsystem/components/RuledBackground.kt` | Canvas drawing modifier (`paperTexture`) for ruled lines and dotted grid. Clean canvas drawing. |
| `StampBadge` | `core/designsystem/components/StampBadge.kt` | Capsule pill badge with 0.5dp border and low-opacity tint. |
| `TagChip` | `core/designsystem/components/TagChip.kt` | Capsule pill tag with 0.5dp border. Uses `Modifier.clickable` with ink ripple. |

### 2.3 Design Tokens & Themes

| Domain | File Path | Tokens Defined |
| :--- | :--- | :--- |
| **Colors** | `core/designsystem/Color.kt` | `PaperColors`: Monochrome core (`MonoBlack`, `MonoWhite`, `MonoGray50` ~ `MonoGray900`, `MonoBlackBg`), Translucent Frosted Glass (`GlassLight`, `GlassLightBar`, `GlassLightFill`, `GlassBorderLight`, `GlassDark`, `GlassDarkBar`, `GlassDarkFill`, `GlassBorderDark`), Slate tints for mood/weather. |
| **Typography** | `core/designsystem/Type.kt` | `PaperTypography`: Apple HIG scale using `SansFontFamily` (`FontFamily.Default`): `displayLarge` (34sp Bold), `displayMedium` (28sp Bold), `titleLarge` (22sp Bold), `titleMedium` (17sp SemiBold), `titleSmall` (15sp SemiBold), `bodyLarge` (17sp Regular), `bodyMedium` (15sp Regular), `bodySmall` (13sp Regular), `labelLarge` (15sp SemiBold), `labelMedium` (12sp Medium), `labelSmall` (11sp Regular). |
| **Shapes** | `core/designsystem/Shape.kt` | `PaperShapes`: `CapsuleShape` (RoundedCornerShape(50)), `small` (10dp), `medium` (16dp), `large` (20dp), `extraLarge` (28dp). |
| **Theme** | `core/designsystem/Theme.kt` | `PaperDiaryTheme`: Sets `LightColorScheme` / `DarkColorScheme`, configures window status bar and navigation bar insets, wraps content in `MaterialTheme`. |

---

## 3. Catalog of Material 3 & Android Idioms in Use

Below is the complete line-by-line audit of Material 3 and Android-specific UI patterns that violate Apple HIG and must be eliminated or refactored:

### 3.1 Material 3 TopAppBar
Static Android `TopAppBar` / `SmallTopAppBar` is currently used across every single screen:
- `TimelineScreen.kt`: Lines 119–188 (`TopAppBar`)
- `SettingsScreen.kt`: Lines 123–140 (`TopAppBar`)
- `EditorScreen.kt`: Lines 160–223 (`TopAppBar`)
- `CalendarScreen.kt`: Lines 66–76 (`TopAppBar`)
- `OnThisDayScreen.kt`: Lines 39–65 (`TopAppBar`)
- `SearchScreen.kt`: Lines 49–126 (`TopAppBar`)
- `StatsScreen.kt`: Lines 41–57 (`TopAppBar`)
- `TrashScreen.kt`: Lines 34–61 (`TopAppBar`)

*HIG Replacement*: Implement `IosLargeTitleScaffold` with dynamic collapsing behavior: 34sp Bold large title smoothly interpolates into a 17sp SemiBold inline centered title on scroll with frosted glass backdrop and 0.5dp hairline border.

### 3.2 Floating Action Button (FAB)
- `TimelineScreen.kt`: Lines 191–205
  ```kotlin
  FloatingActionButton(
      onClick = { onNavigateToEditor(null) },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shape = CircleShape,
      elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
      modifier = Modifier.size(56.dp)
  ) { Icon(Icons.Outlined.Edit, ...) }
  ```
*HIG Replacement*: Completely eliminate the FAB. Place compose action in the iOS navigation bar / toolbar as an authentic Apple Journal navigation action (e.g. top right compose icon or bottom tab bar interaction).

### 3.3 3-Dot Overflow Menus (`Icons.Default.MoreVert` + `DropdownMenu`)
Material 3-dot vertical overflow icon and pop-up dropdown menus are used in two places:
1. `TimelineScreen.kt`: Lines 148–183 (Top bar overflow menu containing "导入 TXT 日记", "数据与统计", "设置")
2. `TimelineScreen.kt`: Lines 440–465 (`DiaryCardItem` per-card menu containing "置顶此篇" / "取消置顶", "移入回收站")

*HIG Replacement*:
- Top bar overflow: Disbanded. Settings, Calendar, and Memories become root tabs on the Bottom Tab Bar. TXT import and Stats move to Settings or dedicated sheet actions.
- Per-card overflow: Replace with iOS contextual interactions — long press context action sheet / menu (`combinedClickable` / pointerInput) and swipe-to-delete/pin.

### 3.4 Ink Ripple Effects
Default Material touch feedback produces expanding radial ink waves:
- `PaperCard.kt`: Line 52 (`Modifier.clickable { onClick() }`)
- `TagChip.kt`: Line 66 (`Modifier.clickable { onClick() }`)
- `TimelineScreen.kt`: Line 367 (`PaperCard(onClick = onClick)`)
- `EditorScreen.kt`: Lines 335, 363 (`StampBadge` clickable), Lines 376, 407 (`Surface(onClick = ...)`), Markdown buttons (Lines 243–299)
- `CalendarScreen.kt`: Line 154 (`Box.clickable` on calendar date cells), Line 246 (`PaperCard(onClick = ...)`)
- `OnThisDayScreen.kt`: Line 118 (`PaperCard(onClick = ...)`)
- `SearchScreen.kt`: Line 262 (`PaperCard(onClick = ...)`)
- `SettingsScreen.kt`: Lines 386, 410, 434, 455, 476 (`Row.clickable`)
- `TrashScreen.kt`: Line 115 (`PaperCard`)
- `LockScreen.kt`: Lines 168, 193, 212 (`clickable` on keypad buttons)

*HIG Replacement*: Create and apply `Modifier.iosClick`:
- Suppress ink ripple (`indication = null`).
- Apply spring physics scale-down (`0.96x ~ 0.98x`).
- Apply subtle alpha dimming (`0.85x`).
- Fire haptic feedback via `LocalHapticFeedback`.

### 3.5 Android Framework & Material Dialogs
- `EditorScreen.kt`: Lines 93–115 (`android.app.DatePickerDialog` and `android.app.TimePickerDialog`)
- `EditorScreen.kt`: Lines 562–594 (`AlertDialog` for adding tags)
- `EditorScreen.kt`: Lines 598–629 (`AlertDialog` for adding location)
- `SettingsScreen.kt`: Lines 494–542 (`AlertDialog` for Supabase credentials)
- `SettingsScreen.kt`: Lines 546–583 (`AlertDialog` for PIN setting)
- `TrashScreen.kt`: Lines 177–211 (`AlertDialog` for emptying trash)

*HIG Replacement*:
- Replace Android dialogs with native iOS Action Sheet modal components (`IosActionSheet`, `IosModalDialog`) featuring 14dp squircle corners, frosted glass background, hairline dividers, separated cancel button, and slide-up transition.
- Replace Android date/time pickers with iOS-style wheel / inline sheet picker.

### 3.6 Material FilterChip, Switch, and Divider Components
- `TimelineScreen.kt`: Lines 221–277 (`FilterChip` for "置顶" and Mood filters)
- `SearchScreen.kt`: Lines 144–209 (`FilterChip` for mood and weather)
- `SettingsScreen.kt`: Lines 251, 298, 322 (`androidx.compose.material3.Switch`)
- `SettingsScreen.kt`: Lines 201, 230, 310, 404, 428, 449, 470 (`HorizontalDivider`)
- `EditorScreen.kt`: Line 254 (`VerticalDivider`)

*HIG Replacement*:
- Replace `FilterChip` rows with `IosSegmentedControl` (pill slider with animated indicator) or iOS capsule tag bars.
- Replace Material `Switch` with iOS-styled toggle switch in `IosSwitchRow`.
- Replace full-width `HorizontalDivider` with 56dp-indented 0.5dp hairline dividers inside `IosListSection`.

---

## 4. Current Navigation Graph Analysis

### 4.1 Root Activity & Lock Screen Architecture (`MainActivity.kt`)
- `MainActivity` inherits from `FragmentActivity` (needed for `BiometricPrompt`).
- Observes `settingsRepository.appLockEnabled`. When active, sets `FLAG_SECURE` on the window to prevent task snapshot leaks.
- Intercepts `onStop`: locks `AppLockManager` unless `AppLockManager.isPickerActive` is true (e.g. file/photo picker opened).
- Directs `setContent` to `PaperDiaryTheme { AppNavigation(...) }`.

### 4.2 Current Navigation Implementation (`AppNavigation.kt`)
- Does **not** use AndroidX Navigation NavHost; instead uses an in-memory mutable state stack:
  ```kotlin
  val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Timeline) }
  ```
- Destinations defined by `sealed interface AppDestination`:
  - `Timeline` (Root)
  - `Editor(val diaryId: String?, val entryDate: Long? = null)`
  - `Calendar`
  - `OnThisDay`
  - `Search`
  - `Stats`
  - `Settings`
  - `Trash`
  - `Lock`
- Screen transitions use a simple fade: `AnimatedContent(targetState = currentDestination, transitionSpec = { fadeIn() togetherWith fadeOut() })`.
- Notice that `NavRoutes.kt` has an unused `sealed class Screen(val route: String)`.

### 4.3 Target Navigation Architecture (R2)
To fulfill R2, navigation must be restructured into a **dual-tier architecture**:
1. **Tier 1: Root Tab Navigation (`IosTabBar`)**
   A bottom translucent tab bar hosting 4 primary tabs:
   - Tab 1: **Journal** (`TimelineScreen`)
   - Tab 2: **Calendar** (`CalendarScreen`)
   - Tab 3: **Memories** (`OnThisDayScreen`)
   - Tab 4: **Settings** (`SettingsScreen`)
2. **Tier 2: Modal & Detail Push Destinations**
   - `Editor(diaryId, entryDate)`: Slide-up modal presentation.
   - `Search`: Full-screen search with iOS back or cancel.
   - `Stats`: Detail pushed from Settings (or accessible via Journal).
   - `Trash`: Detail pushed from Settings with Inset Grouped list.
   - `Lock`: Modal overlay over entire window when locked.

---

## 5. Detailed File Boundaries & Refactoring Roadmap

### 5.1 Category 1: New Files to Create (iOS Primitives & Components)

| File to Create | Directory | Purpose & Contents |
| :--- | :--- | :--- |
| `IosTouchPhysics.kt` | `core/designsystem/` | `Modifier.iosClick()`: pointer press detection, spring scale (0.97x), alpha dim (0.85x), haptic feedback (`LocalHapticFeedback`), `indication = null`. |
| `IosListComponents.kt` | `core/designsystem/components/` | `IosListSection` (inset grouped card, 16dp rounded squircle, title/footer text), `IosListRow` (base row with 30dp squircle icon box, 56dp indented 0.5dp divider), `IosNavigationRow` (chevron disclosure accessory), `IosSwitchRow` (iOS toggle switch). |
| `IosSegmentedControl.kt` | `core/designsystem/components/` | Apple HIG segmented pill control with animated sliding white/glass indicator for scoped timeline/search filtering. |
| `IosTabBar.kt` | `core/designsystem/components/` | 4-tab bar (Journal, Calendar, Memories, Settings) with 93% translucency (`GlassLightBar`/`GlassDarkBar`), 0.5dp hairline top border, SF-style icons, active tint. |
| `IosLargeTitleScaffold.kt` | `core/designsystem/components/` | Dynamic collapsible header scaffold: 34sp Bold Large Title transitioning smoothly into 17sp SemiBold centered inline title on scroll, with frosted glass background. |
| `IosActionSheet.kt` | `core/designsystem/components/` | iOS-native modal action sheet: frosted glass card, squircle corners, separated cancel button, hairline dividers, slide-up animation. |
| `IosDatePicker.kt` | `core/designsystem/components/` | iOS-style wheel / compact sheet date & time picker replacing Android framework dialogs. |

### 5.2 Category 2: Existing Files to Refactor

| Target File | Modifications Required |
| :--- | :--- |
| `core/designsystem/AppleMaterial.kt` | Validate and refine `glassBorder` (0.5dp specular gradient) and material background brushes for light/dark modes. |
| `core/designsystem/components/PaperCard.kt` | Replace `Modifier.clickable` with `Modifier.iosClick` to eliminate Material ripple. Support squircle shape. |
| `core/designsystem/Theme.kt` | Override `LocalIndication` to provide null or iOS indication, ensuring zero Material ink ripple across the app. |
| `ui/navigation/AppNavigation.kt` | Implement 4-tab root bar (`IosTabBar`) holding Journal, Calendar, Memories, Settings. Manage sub-screen navigation (Editor, Search, Stats, Trash) and Lock overlay. Clean up obsolete references. |
| `ui/navigation/NavRoutes.kt` | Update route models or deprecate `Screen` enum in favor of clean type-safe tab and destination sealed interfaces. |
| `ui/timeline/TimelineScreen.kt` | - Remove `TopAppBar`, FAB, and 3-dot overflow menu.<br>- Implement `IosLargeTitleScaffold` with "日记" title.<br>- Add navigation bar compose button ("写日记" / `Icons.Outlined.Edit`).<br>- Integrate `IosSegmentedControl` for pinned / mood filtering.<br>- Update `DiaryCardItem` to use `iosClick` and contextual long-press action sheet for pin/delete. |
| `ui/settings/SettingsScreen.kt` | - Rebuild entire screen using `IosListSection`, `IosListRow`, `IosNavigationRow`, `IosSwitchRow`.<br>- Eliminate Material `TopAppBar` in favor of iOS header.<br>- Replace `AlertDialog` for Supabase credentials and PIN setup with `IosActionSheet` / `IosModalDialog`. |
| `ui/editor/EditorScreen.kt` | - Redesign top toolbar: iOS Cancel / Done navigation actions, compact iOS date button.<br>- Replace Android `DatePickerDialog` / `TimePickerDialog` with `IosDatePicker`.<br>- Replace Material `AlertDialog` (tag, location) with `IosModalDialog`.<br>- Apply `iosClick` to Markdown toolbar items. |
| `ui/calendar/CalendarScreen.kt` | - Adapt as a root tab: remove back button when hosted in Tab Bar.<br>- Add iOS large title header.<br>- Apply `iosClick` to calendar date cells. |
| `ui/onthisday/OnThisDayScreen.kt` | - Adapt as "Memories" root tab: remove back button when hosted in Tab Bar.<br>- Add iOS large title header.<br>- Apply `iosClick` to memory cards. |
| `ui/search/SearchScreen.kt` | - Align with iOS Search style: search bar with Cancel button, segmented filter bar.<br>- Apply `iosClick` to results. |
| `ui/stats/StatsScreen.kt` | - Structure using iOS Inset Grouped sections.<br>- Apply `iosClick` and iOS typography. |
| `ui/trash/TrashScreen.kt` | - Replace Material `AlertDialog` for emptying trash with `IosActionSheet` (destructive action style).<br>- Apply `iosClick`. |
| `ui/lock/LockScreen.kt` | - Refine 3x4 keypad buttons with `iosClick` spring physics. |

### 5.3 Category 3: Untouched Files (Zero Regression Guarantee for R4)

The following core non-UI modules MUST remain 100% intact:
- **Database & DAOs**:
  - `core/database/AppDatabase.kt`
  - `core/database/dao/DiaryDao.kt`
  - `core/database/dao/TagDao.kt`
  - `core/database/dao/AttachmentDao.kt`
  - `core/database/entity/*` (`DiaryEntity.kt`, `TagEntity.kt`, `AttachmentEntity.kt`, `DiaryTagCrossRef.kt`, `DiaryWithDetails.kt`)
- **Security & Cryptography**:
  - `core/security/AppLockManager.kt`
  - `core/security/BiometricHelper.kt`
  - `core/security/PinCipher.kt`
- **Sync & Backup**:
  - `core/sync/SyncManager.kt`
  - `core/sync/SyncWorker.kt`
  - `core/network/SupabaseClient.kt`
  - `core/backup/BackupManager.kt`
  - `core/backup/TxtDiaryImporter.kt`
- **Data Repositories**:
  - `data/repository/DiaryRepository.kt`
  - `data/repository/MediaRepository.kt`
  - `data/repository/SettingsRepository.kt`
- **Domain Models**:
  - `domain/model/Diary.kt`, `Attachment.kt`, `Mood.kt`, `Weather.kt`, `Stats.kt`, `Tag.kt`, `SyncStatus.kt`
- **ViewModels**:
  - `ui/timeline/TimelineViewModel.kt`
  - `ui/editor/EditorViewModel.kt`
  - `ui/calendar/CalendarViewModel.kt`
  - `ui/onthisday/OnThisDayViewModel.kt`
  - `ui/search/SearchViewModel.kt`
  - `ui/settings/SettingsViewModel.kt`
  - `ui/stats/StatsViewModel.kt`
  - `ui/trash/TrashViewModel.kt`
  - `ui/lock/LockViewModel.kt`

---

## 6. Verification Plan & Quality Criteria

### 6.1 Compilation Verification
- Command: `./gradlew assembleDebug`
- Expected: 0 compiler warnings/errors, binary APK generated successfully.

### 6.2 Test Suite Verification
- Command: `./gradlew test`
- Expected: 100% of unit tests pass (`DiaryModelTest`, `BackupManagerTest`, `TxtDiaryImporterTest`).

### 6.3 HIG Fidelity Inspection Checkpoints
1. **Tab Bar**: Verify 4 tabs (`Journal`, `Calendar`, `Memories`, `Settings`) render with 93% translucency and 0.5dp top border. Switching tabs preserves scroll/state.
2. **No Android Idioms**:
   - Grep search confirms 0 occurrences of `FloatingActionButton` on main user journey.
   - Grep search confirms 0 occurrences of `Icons.Default.MoreVert` on main user journey.
   - Grep search confirms 0 occurrences of Android `DatePickerDialog` / `TimePickerDialog`.
3. **Touch Physics**:
   - Tapping diary cards, buttons, or list rows produces smooth spring scale-down (`~0.97x`) and alpha dimming with haptic click, with 0 radial ink ripple visible.
4. **Settings Screen**:
   - Renders strictly as iOS Inset Grouped lists with 16dp rounded corners, 30dp squircle icon containers, and 56dp indented dividers.
5. **Timeline Screen**:
   - Scrolling produces smooth interpolation from 34sp Bold Large Title into centered 17sp inline title.
   - Segmented control pills toggle filter state smoothly.
