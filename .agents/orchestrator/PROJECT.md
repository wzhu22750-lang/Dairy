# Project: InkPaperDiary Apple HIG Refactoring

## Architecture
Transform the Android Jetpack Compose diary application (`com.example.inkpaperdiary`) into an authentic Apple Human Interface Guidelines (HIG) architecture:
1. **Design System Layer (`core/designsystem`)**:
   - `AppleMaterial.kt`: Dynamic translucent materials (5 thicknesses) + 0.5dp specular hairline glass borders (`AppleMaterials.glassBorder`).
   - `IosTouchPhysics.kt`: iOS-native spring physics (`Modifier.iosClick` scale 0.97f, alpha 0.85f, haptic click, zero ripple).
   - `IosListComponents.kt`: Inset Grouped list containers (`IosListSection`, `IosListRow`, `IosNavigationRow`, `IosSwitchRow`) with 16dp squircle corners, 30dp squircle icon box, 56dp indented divider.
   - `IosSegmentedControl.kt`: Seamless sliding pill filter selector.
   - `IosActionSheet.kt` & `IosModalDialog.kt` & `IosDateTimePickerSheet.kt`: iOS-native sheets and dialogs.
2. **Navigation & Scaffolding Layer (`ui/navigation`, `core/designsystem/scaffold`)**:
   - `IosTabBar.kt`: 4-tab bottom navigation (`Journal`, `Calendar`, `Memories`, `Settings`) with 93% translucency and hairline top border.
   - `IosLargeTitleScaffold.kt`: Dynamic scroll-coupled header (34sp Bold Large Title transitioning to centered 17sp SemiBold inline title with frosted glass elevation at 52dp scroll offset).
   - `AppNavigation.kt`: 2-tier navigation structure (Root 4-tab bar + modal push for Editor, Search, Stats, Trash, Lock).
3. **Screen Presentation Layer (`ui/*`)**:
   - `TimelineScreen`: Apple Journal-style stream with segmented filters, spring-press cards, top-right compose action, contextual action sheets.
   - `SettingsScreen`: 4-section Inset Grouped list matching iOS Settings.
   - `EditorScreen`: iOS navigation bar with Cancel/Done text actions, inline capsule date/time picker pill.
   - Secondary screens (`CalendarScreen`, `OnThisDayScreen`, `SearchScreen`, `StatsScreen`, `TrashScreen`, `LockScreen`): Consistent iOS top bar, spring feedback, zero ripples.
4. **Domain & Data Protection Layer (UNTOUCHED)**:
   - All Room entities, DAOs, repositories, Security (`AppLockManager`, `PinCipher`), and Sync (`SupabaseClient`, `SyncManager`) remain 100% intact.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Translucent Materials & Vibrancy | 5-level MaterialThickness + 4-tier Vibrancy + 0.5dp glassBorder | M1 | R1 |
| 2 | iOS Touch Physics (`iosClick`) | Spring scale-down (0.97x), alpha (0.85x), haptic, zero ink ripple | M1 | R1 |
| 3 | System Inset Grouped List Components | IosListSection (16dp squircle), IosListRow (30dp icon, 56dp divider), IosNavigationRow, IosSwitchRow | M1 | R1 |
| 4 | iOS Segmented Control | Pill slider with animated thumb and spring physics | M1 | R1 |
| 5 | Bottom Translucent Tab Bar (`IosTabBar`) | 4-tab bar (Journal, Calendar, Memories, Settings) with 93% translucency and hairline border | M2 | R2 |
| 6 | Dynamic Collapsible Large Title | 34sp Bold Large Title transitioning to centered 17sp SemiBold inline title with frosted glass | M2 | R2 |
| 7 | Android Idiom Elimination (Root) | Complete removal of FAB and top-right 3-dot overflow menu (Icons.Default.MoreVert) | M2 | R2 |
| 8 | 2-Tier Root Navigation Architecture | 4 root tabs + modal push for Editor, Search, Stats, Trash, Lock | M2 | R2 |
| 9 | Timeline Screen Overhaul | Apple Journal stream, segmented filters, spring cards, compose in top bar, long-press action sheet | M3 | R3 |
| 10 | Settings Screen Inset Grouped Overhaul | 4 Inset Grouped sections (Sync, Security, Style, Data) with squircle icons & indented dividers | M4 | R3 |
| 11 | iOS Modal Action Sheets & Dialogs | IosActionSheet (bottom sheet with cancel pill), IosModalDialog (270dp alert), replacing Android dialogs | M4 | R3 |
| 12 | Editor Screen iOS Navigation & Toolbar | Cancel/Done text buttons, inline capsule date/time pill, markdown bar | M5 | R3 |
| 13 | iOS Date/Time Picker Sheet | IosDateTimePickerSheet replacing Android DatePickerDialog/TimePickerDialog | M5 | R3 |
| 14 | Secondary Screens HIG Polish | Calendar, Memories, Search, Stats, Trash using iOS headers & spring touch | M5 | R3 |
| 15 | Non-UI Business Logic & Room Protection | 100% preservation of Room DAOs, ViewModels, Security (isPickerActive), Supabase sync | M6 | R4 |
| 16 | E2E Testing & Acceptance Gate | 100% pass of opaque-box E2E test suite + assembleDebug 0 errors | M6 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | iOS Design System & Interaction Primitives | AppleMaterial, iosClick, IosListComponents, IosSegmentedControl, PaperCard | none | DONE |
| M2 | Root Navigation & Collapsible Large Title | IosTabBar, IosLargeTitleScaffold, AppNavigation 2-tier refactor | M1 | PLANNED |
| M3 | Timeline Screen Overhaul | TimelineScreen Journal stream, compose in bar, segmented filters, card action sheet | M2 | PLANNED |
| M4 | Settings Screen & Modal Sheets/Dialogs | SettingsScreen Inset Grouped, IosActionSheet, IosModalDialog | M1, M2 | PLANNED |
| M5 | Editor & Secondary Screens Polish | EditorScreen Cancel/Done, IosDateTimePickerSheet, Calendar/Memories/Trash | M3, M4 | PLANNED |
| M6 | Final Verification & Coverage Hardening | 100% E2E test pass, assembleDebug, adversarial review, forensic audit | M1-M5 | PLANNED |

## Interface Contracts
### `Modifier.iosClick`
```kotlin
fun Modifier.iosClick(
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.97f,
    pressedAlpha: Float = 0.85f,
    onClick: () -> Unit
): Modifier
```

### `IosListComponents`
```kotlin
@Composable
fun IosListSection(
    title: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)

@Composable
fun IosListRow(
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
)
```

### `IosTabBar`
```kotlin
enum class IosTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    JOURNAL("日记", Icons.Outlined.Book, Icons.Filled.Book),
    CALENDAR("日历", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    MEMORIES("回忆", Icons.Outlined.History, Icons.Filled.History),
    SETTINGS("设置", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun IosTabBar(
    selectedTab: IosTab,
    onTabSelected: (IosTab) -> Unit,
    modifier: Modifier = Modifier
)
```

### `IosLargeTitleScaffold`
```kotlin
@Composable
fun IosLargeTitleScaffold(
    title: String,
    scrollState: ScrollState,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
)
```

## Code Layout
### Modified Files:
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/AppleMaterial.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/PaperCard.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/theme/Theme.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/navigation/NavRoutes.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/editor/EditorScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/calendar/CalendarScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/onthisday/OnThisDayScreen.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/trash/TrashScreen.kt`

### New Files to Create:
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/interaction/IosTouchPhysics.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosListComponents.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosSegmentedControl.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosActionSheet.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosModalDialog.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/IosDatePicker.kt`
- `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`
- `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`

### Protected Files (UNTOUCHED):
- `app/src/main/java/com/example/inkpaperdiary/core/database/**`
- `app/src/main/java/com/example/inkpaperdiary/core/security/**`
- `app/src/main/java/com/example/inkpaperdiary/core/sync/**`
- `app/src/main/java/com/example/inkpaperdiary/core/backup/**`
- `app/src/main/java/com/example/inkpaperdiary/core/network/**`
- `app/src/main/java/com/example/inkpaperdiary/data/repository/**`
