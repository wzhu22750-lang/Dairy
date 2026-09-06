# Original User Request

## Initial Request — 2026-09-06T18:26:23+08:00

Thoroughly refactor the Android Jetpack Compose diary application (`com.example.inkpaperdiary`) to eliminate Android/Material 3 idioms and adopt an authentic Apple Human Interface Guidelines (HIG) layout and component architecture.

Working directory: `/Users/kuangqie/Documents/VibeCoding/日记本`
Integrity mode: development

## Requirements

### R1. iOS Design System & Interaction Primitives
Implement foundational iOS-native UI primitives in Jetpack Compose without introducing external third-party UI libraries:
- **Materials & Vibrancy**: Dynamic translucent materials (ultra-thin to ultra-thick) with 0.5dp hairline specular gradient borders (`AppleMaterials.glassBorder`).
- **iOS Touch Physics (`iosClick`)**: Replace Material ink ripples with iOS-native press feedback: spring scale-down (0.96~0.98x), subtle alpha dimming (0.85x), and haptic click feedback (`LocalHapticFeedback`).
- **System Inset Grouped List Components**: Standardize `IosListSection`, `IosListRow`, `IosNavigationRow`, and `IosSwitchRow` with 14~16dp squircle corners, left icon boxes (30dp with squircle background), and 56dp indented 0.5dp dividers.
- **iOS Segmented Control**: Seamless pill slider with animated indicator for scoped filtering.

### R2. Root Navigation Architecture & Collapsible Large Title
Replace the Android top-bar-centric navigation with iOS-native structure:
- **Bottom Translucent Tab Bar**: Implement a 4-tab bar (`Journal`, `Calendar`, `Memories`, `Settings`) with 93% translucency and hairline top border.
- **Dynamic Collapsible Large Title**: Replace static `TopAppBar` with a scrolling header that smoothly transitions from a 34sp Bold Large Title into a centered 17sp SemiBold inline title with frosted glass elevation.
- **Eliminate Android Idioms**: Completely remove the Floating Action Button (FAB) and the top-right 3-dot overflow menu (`Icons.Default.MoreVert`).

### R3. Screen Layout & Component Overhaul
Thoroughly reconstruct screen layouts:
- **TimelineScreen**: Transform into an Apple Journal-style stream with segmented control filters, spring-press diary cards, and contextual long-press actions. Move compose actions to the navigation bar/toolbar.
- **SettingsScreen**: Rebuild entirely using `IosListSection` and `IosListRow` to match the native iOS Settings app hierarchy.
- **EditorScreen & Dialogs**: Redesign toolbar, date/time pickers, and replace standard Android alert dialogs with iOS-style modal action sheets.

### R4. Business Logic Preservation & Zero Regression
Guarantee that all non-UI domains remain 100% intact and functional:
- Room database entities and DAOs (`DiaryDao`, `TagDao`, `AttachmentDao`).
- AppLockManager, Biometrics, and PIN security.
- Cloud synchronization (`SyncManager`, `SyncWorker`, Supabase client) and backup/import pipelines.

## Acceptance Criteria

### Verification & Compilation
- [ ] `./gradlew assembleDebug` compiles successfully with 0 errors.
- [ ] No regression in ViewModel state flows or Room data transactions.

### Layout & Component Fidelity
- [ ] No Material 3 FAB or 3-dot overflow menus exist on the primary user journey.
- [ ] Main navigation transitions fluidly across the 4 tabs via `IosTabBar`.
- [ ] Interactive elements (cards, buttons, rows) exhibit iOS spring scale-down feedback rather than Material ripple effects.
- [ ] Settings screen renders strictly as an Inset Grouped list with indented dividers and squircle icons.
- [ ] Timeline screen displays collapsible large title and segmented filters.
