# Test Infrastructure: Apple HIG Refactoring Test Suite

**Project:** InkPaperDiary (`com.example.inkpaperdiary`)  
**Version:** 1.0.0  
**Test Suite Target:** Apple HIG Refactoring & Domain Integrity Verification  
**Author:** E2E Test Writer (`teamwork_preview_test_writer`)  
**Status:** Active  

---

## 1. Test Philosophy

The testing infrastructure is built upon the following core tenets:

1. **Opaque-Box Architectural Verification:**
   Tests validate observable contracts, mathematical formulas, design tokens, navigation routes, and state flows rather than brittle implementation internals.
2. **Four-Tier Testing Methodology:**
   - **Tier 1 (Feature Coverage):** Minimum of 5 isolated test cases per feature across R1 (Design System), R2 (Navigation), R3 (Screen Layout), and R4 (Domain & Data Preservation).
   - **Tier 2 (Boundary & Corner Cases):** Minimum of 5 edge/stress test cases per feature verifying clamping, nullability, overflow, extreme values, and exception recovery.
   - **Tier 3 (Cross-Feature Combinations):** Pairwise interaction tests validating cross-cutting concerns (e.g. TabBar + AppLock, SegmentedControl + Touch Physics, Settings + Supabase Dialog, DatePicker + Markdown WordCount).
   - **Tier 4 (Real-World Application Scenarios):** Minimum of 5 comprehensive, end-to-end multi-step user journey workflows mirroring actual production use.
3. **Hermetic, Fast, Zero-Emulator Execution:**
   All unit and functional test suites run directly on the local JVM via `./gradlew test` using standard JUnit 4 and Kotlin Coroutines Test, executing in seconds without requiring Android emulator orchestration.
4. **Adversarial Regression & Idiom Purge Auditing:**
   Automated static and structural audits enforce zero tolerance for deprecated Android Material 3 idioms (`FloatingActionButton`, `Icons.Default.MoreVert`, radial ink ripples) on primary user flows.
5. **Domain & Data Protection Guarantee:**
   Strict verification that Room entity schemas, DAOs, AppLockManager state flows, PIN cipher contracts, and Supabase sync payloads maintain 100% backward compatibility and zero data loss.

---

## 2. Feature Inventory

| ID | Category | Feature Name | Description | Key Interface / Specification |
|---|---|---|---|---|
| **F1** | R1 Design System | Translucent Materials & Vibrancy | 5-level `MaterialThickness` + 4-tier `VibrancyLevel` + 0.5dp hairline specular `glassBorder` | `AppleMaterials.kt`, `AppleMaterial.glassBorder` |
| **F2** | R1 Interaction | iOS Touch Physics (`iosClick`) | Spring scale-down (0.97x), alpha dimming (0.85x), spring damping (0.75f/400f), zero ripple | `Modifier.iosClick`, `IosTouchPhysics.kt` |
| **F3** | R1 Inset Lists | System Inset Grouped Lists | 16dp rounded card, 30dp squircle icon box (7dp corner), 56dp indented divider (16dp fallback) | `IosListSection`, `IosListRow`, `IosNavigationRow`, `IosSwitchRow` |
| **F4** | R1 Controls | iOS Segmented Control | 32dp pill track (9dp radius), 7dp thumb indicator with drop shadow, 13sp SemiBold typography | `IosSegmentedControl.kt` |
| **F5** | R2 Navigation | Bottom Translucent Tab Bar | 4 root tabs (Journal, Calendar, Memories, Settings), 49dp height, 93% frosted glass, 0.5dp top border | `IosTabBar.kt`, `IosTab` enum |
| **F6** | R2 Navigation | Collapsible Large Title | 34sp Bold title smoothly transitioning to 17sp SemiBold inline title over 52dp scroll offset | `IosLargeTitleScaffold.kt`, offset interpolation formula |
| **F7** | R2 Idioms | Android Idiom Elimination | Complete purge of FAB and 3-dot overflow menu (`Icons.Default.MoreVert`); actions moved to nav bar | Screen layouts, structural purge audit |
| **F8** | R2 Navigation | 2-Tier Navigation Architecture | 4 root bottom tabs + modal push routes (Editor, Search, Stats, Trash, Lock) | `NavRoutes.kt`, `Screen` sealed hierarchy |
| **F9** | R3 Timeline | Apple Journal Stream | Journal stream with segmented filter, 16dp squircle diary cards, 3dp pinned accent bar, long-press action sheet | `TimelineScreen.kt`, `PaperCard.kt` |
| **F10** | R3 Settings | Inset Grouped Settings | 4 grouped sections (Cloud, Security, Paper Style, Data Management) with squircle icons and switches | `SettingsScreen.kt`, `SettingsUiState` |
| **F11** | R3 Dialogs | iOS Modal Action Sheets & Dialogs | `IosActionSheet` (14dp squircle group + detached cancel pill) & `IosModalDialog` (270dp fixed width) | `IosActionSheet.kt`, `IosModalDialog.kt` |
| **F12** | R3 Editor | Editor Navigation & Toolbar | Cancel/Done text actions, center capsule date/time button, bottom markdown formatting bar | `EditorScreen.kt`, `EditorViewModel` |
| **F13** | R3 Pickers | iOS Date/Time Picker Sheet | Modal bottom sheet with Cancel, Title, and Done actions, replacing Android Date/TimePickerDialog | `IosDateTimePickerSheet.kt` |
| **F14** | R3 Polish | Secondary Screens HIG Polish | Calendar, Memories, Search, Stats, Trash adhering to iOS typography, cards, and spring touch | Secondary UI screens |
| **F15** | R4 Domain | Room Entities & Data Contracts | Room `DiaryEntity`, `AttachmentEntity`, `TagEntity`, `DiaryTagCrossRef`, `DiaryWithDetails` | `core/database/**`, table indices & schema |
| **F16** | R4 Security | AppLock & Privacy Lifecycle | `AppLockManager` StateFlow, `isPickerActive` flag preventing lock on external system pickers | `AppLockManager.kt` |
| **F17** | R4 Cloud & Data | Cloud Sync & Backup Pipelines | `SupabaseClient` headers, REST endpoints, `BackupManager` JSON cycle, `TxtDiaryImporter` charset fallback | `core/network/**`, `core/backup/**` |

---

## 3. Test Architecture

### 3.1 Directory & Package Structure
```
app/src/test/java/com/example/inkpaperdiary/
├── BackupManagerTest.kt                 # [Existing] Domain JSON round-trip test
├── DiaryModelTest.kt                    # [Existing] Diary word count & model fallback test
├── TxtDiaryImporterTest.kt              # [Existing] TXT parser & GB18030 charset test
│
├── tier1_features/                      # Tier 1: Core Feature Coverage (>=5 tests per feature)
│   ├── R1DesignSystemFeatureTest.kt     # F1 (Materials/Vibrancy), F2 (Touch Physics), F3 (Inset Lists), F4 (Segmented Control)
│   ├── R2NavigationFeatureTest.kt       # F5 (TabBar), F6 (LargeTitle Scaffold), F7 (Idiom Purge), F8 (2-Tier Nav Routes)
│   ├── R3ScreenLayoutFeatureTest.kt     # F9 (Timeline Stream), F10 (Settings), F11 (Sheets/Alerts), F12 (Editor), F13 (Picker), F14 (Secondary)
│   └── R4BusinessLogicFeatureTest.kt    # F15 (Room Entities), F16 (AppLockManager), F17 (Supabase & Backup Data)
│
├── tier2_boundaries/                    # Tier 2: Boundary & Corner Cases (>=5 tests per feature)
│   ├── R1BoundaryEdgeCasesTest.kt       # Negative alpha, drag cancel, single-row card, null-icon indent
│   ├── R2BoundaryEdgeCasesTest.kt       # Scroll overscroll clamp, rapid tab switch, route param encoding
│   ├── R3BoundaryEdgeCasesTest.kt       # Text ellipsis overflow, massive attachments, leap-year timestamps
│   └── R4BoundaryEdgeCasesTest.kt       # Malformed JSON, empty TXT, concurrent lock, missing sync config
│
├── tier3_combinations/                  # Tier 3: Cross-Feature Combinations (Pairwise matrix)
│   └── CrossFeaturePairwiseTest.kt      # Pairwise interactions across R1 x R2 x R3 x R4
│
└── tier4_scenarios/                     # Tier 4: Real-World Application Scenarios (>=5 multi-step flows)
    └── RealWorldApplicationScenariosTest.kt # Scenario 1 (First Entry), Scenario 2 (Cloud Sync), Scenario 3 (Lockout/Picker),
                                             # Scenario 4 (Backup/Trash Migration), Scenario 5 (Heavy Journal Filtering)
```

### 3.2 Toolchain & Execution Commands
- **Framework:** JUnit 4 (`libs.junit`), KotlinX Coroutines Test (`libs.kotlinx.coroutines.test`)
- **JVM Target:** Java 17 / Kotlin 2.0+
- **Execution Command:** `./gradlew test` (or `./gradlew testDebugUnitTest --info`)
- **Report Location:** `app/build/reports/tests/testDebugUnitTest/index.html`

---

## 4. Scenarios

### Tier 1: Feature Coverage Specifications
- **R1:**
  - `testMaterialThicknessColors`: Verifies RGB/Alpha matrices for all 5 thickness levels in light and dark modes.
  - `testVibrancyAlphaScaling`: Verifies 100%, 60%, 30%, 18% alpha levels for primary, secondary, tertiary, and quaternary vibrancy.
  - `testGlassBorderSpecs`: Verifies 0.5dp hairline width and gradient brush specifications.
  - `testTouchPhysicsSpecs`: Validates 0.97f scale-down, 0.85f alpha attenuation, 0.75f damping ratio, 400f stiffness, and null ripple.
  - `testInsetGroupedGeometry`: Verifies 16dp container radius, 30dp squircle icon box, 7dp icon corner, and 56dp indented divider formula.
  - `testSegmentedControlGeometry`: Verifies 32dp height, 9dp corner radius, 7dp thumb radius, and 13sp SemiBold typography.
- **R2:**
  - `testTabBarTabDefinitions`: Verifies 4 canonical tabs (Journal, Calendar, Memories, Settings) and 49dp content height.
  - `testLargeTitleScrollCollapseFormula`: Verifies mathematical alpha interpolation `(offset / maxOffset).coerceIn(0f, 1f)` across 52dp threshold.
  - `testMaterialIdiomEliminationRules`: Verifies absence of FAB and 3-dot overflow menu on primary navigation.
  - `testNavRoutesContracts`: Verifies sealed screen routes, editor route parameter generation (`editor/new`, `editor/{id}`), and route matching.
- **R3:**
  - `testTimelineJournalStreamStructure`: Validates segmented filter options (全部, 置顶, 心情), 16dp squircle diary cards, and 3dp pinned accent indicator.
  - `testSettingsFourSectionHierarchy`: Verifies exact 4 sections (Cloud & Sync, Security & Privacy, Paper Texture, Data Management).
  - `testIosActionSheetAndModalDialogSpecs`: Verifies 270dp modal dialog width, 14dp squircle corners, 44dp button height, 56dp action sheet row height, and red destructive styling.
  - `testEditorToolbarAndPickerSpecs`: Verifies Cancel/Done text button styling and inline date capsule picker contract.
- **R4:**
  - `testDiaryEntityRoomIndicesAndDefaults`: Verifies 4 database indices, default syncStatus = 1 (DIRTY), and non-null UUID generation.
  - `testAppLockManagerLifecycle`: Verifies `isLocked` StateFlow, lock/unlock transitions, and `isPickerActive` flag.
  - `testSupabaseClientContractAndHeaders`: Verifies auth headers, apikey injection, URL sanitization, and sync payloads.

### Tier 2: Boundary & Corner Cases Specifications
- Extreme scroll offsets (negative overscroll, massive 10,000px flings) ensuring title alpha remains strictly within `[0f, 1f]`.
- Gesture drag-out cancellation ensuring scale smoothly returns to 1.0f without firing click callback.
- Single-row `IosListSection` verifying both top and bottom corners are rounded and bottom divider is omitted.
- Null icon in `IosListRow` collapsing divider start indent from 56dp to 16dp.
- Text overflow in settings rows with long URLs and timestamps exercising `TextOverflow.Ellipsis`.
- Massive diary attachments (0, 1, 3, 20 items) verifying horizontal thumbnail constraints.
- Timestamp boundaries (Unix epoch 0, leap year Feb 29, Year 2038) in date pickers.
- Corrupted JSON and malformed TXT imports verifying safe error recovery without application crash.

### Tier 3: Cross-Feature Combinations (Pairwise)
- **Pair 1:** Bottom TabBar tab navigation during active AppLock state.
- **Pair 2:** Timeline SegmentedControl mood filtering combined with `PaperCard` spring touch physics.
- **Pair 3:** Settings Inset Grouped navigation row click launching Supabase credentials `IosModalDialog`.
- **Pair 4:** Editor screen Cancel/Done navigation combined with date picker update and word count calculation.
- **Pair 5:** Diary card long-press invoking `IosActionSheet` combined with move-to-trash soft deletion and sync status update.
- **Pair 6:** System photo/document picker invocation (`isPickerActive = true`) during import, preventing `AppLockManager` background auto-lock.
- **Pair 7:** Dynamic theme mode switch (Light <-> Dark) combined with Apple HIG material translucency and specular glass border recalculation.
- **Pair 8:** Collapsible large title scroll offset changes combined with segmented control thumb animation.

### Tier 4: Real-World Application Scenarios
1. **First Diary Creation Flow:** New user launches app -> navigates to Editor via top-right Compose action -> selects date and mood -> writes Markdown content -> saves entry -> verifies entry in Journal stream with correct preview, word count, and card styling.
2. **Cloud Synchronization Lifecycle:** User configures Supabase credentials via Settings modal dialog -> triggers manual sync -> validates headers and sync payload -> verifies sync status transitions from DIRTY to SYNCED.
3. **Privacy Security & External Attachment Flow:** AppLock is enabled -> user opens Editor -> triggers external media picker -> verifies `isPickerActive = true` prevents lockout -> completes selection -> returns to background -> verifies app immediately re-locks.
4. **Data Management, Trash & Lossless Migration:** User soft-deletes diary -> verifies moved to Trash -> recovers diary -> exports JSON backup -> resets database -> imports backup -> verifies 100% field preservation.
5. **Heavy Power-User Journal Stream Interaction:** 100+ diaries loaded -> filter stream by SegmentedControl "置顶" -> rapid scrolling triggers LargeTitle collapse and expansion -> long-press invokes ActionSheet to toggle pin status.

---

## 5. Quality Thresholds & Gates

| Metric | Target / Threshold | Gate Status |
|---|---|---|
| **Compilation Success** | 0 errors (`./gradlew test` & `./gradlew assembleDebug`) | **STRICT (Blocking)** |
| **Unit Test Pass Rate** | **100% Pass** (0 failures, 0 errors, 0 skipped) | **STRICT (Blocking)** |
| **Test Execution Time** | < 15 seconds total suite execution | **ACCEPTABLE** |
| **Tier 1 Coverage** | >= 5 tests per feature (F1 - F17) | **STRICT (Blocking)** |
| **Tier 2 Boundary Coverage** | >= 5 tests per feature | **STRICT (Blocking)** |
| **Tier 3 Combinations** | >= 8 comprehensive pairwise scenarios | **STRICT (Blocking)** |
| **Tier 4 Scenarios** | >= 5 real-world end-to-end application flows | **STRICT (Blocking)** |
| **Material Idiom Purge** | 0 FABs, 0 3-dot menus, 0 ink ripples in primary UI | **STRICT (Blocking)** |
| **Domain Schema Preservation** | 0 breaking changes to Room entities / DAOs | **STRICT (Blocking)** |
