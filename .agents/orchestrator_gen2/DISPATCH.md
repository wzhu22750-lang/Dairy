# Dispatch — Project Orchestrator (Generation 2)

## 2026-09-06T18:57:17+08:00
You are the Project Orchestrator.

Your working directory is: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator_gen2`
Project root directory is: `/Users/kuangqie/Documents/VibeCoding/日记本`
The authoritative user request is in: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`

## Mission
Continue and complete the Apple Human Interface Guidelines (HIG) architectural refactoring of the Android Jetpack Compose diary application (`com.example.inkpaperdiary`), strictly satisfying all remaining requirements and acceptance criteria.

## Current State & Context
- **Milestone 1 (DONE & VERIFIED)**: Core iOS primitives (`AppleMaterial`, `IosTouchPhysics`, `IosListComponents`, `IosSegmentedControl`, `PaperCard`) are implemented and passed test suites. See `/Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md` and `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/orchestrator/GATE_STATUS.md`.
- **Testing Infrastructure**: 152 test cases passing. See `TEST_INFRA.md` and `TEST_READY.md`.

## Remaining Requirements to Drive to Completion:
1. **R1. Root Navigation Architecture & Collapsible Large Title (M2)**:
   - Complete `IosTabBar` 4-tab bottom navigation (`Journal`, `Calendar`, `Memories`, `Settings`) with 93% translucency and hairline top border.
   - Integrate `IosLargeTitleTopBar` and `IosLargeTitleScaffold` across root screens (34sp Bold title transitioning to centered 17sp SemiBold title upon scroll).
   - Complete 2-tier root navigation in `AppNavigation.kt` (4 tabs at root + pushed modals).
   - Eliminate all Material 3 FABs and top-right 3-dot overflow menus (`Icons.Default.MoreVert`).
2. **R2. Screen Layout & Component Overhaul (M3 - M5)**:
   - **TimelineScreen**: Apple Journal-style stream with segmented control filters, spring-press cards, top-right compose action, contextual action sheets.
   - **SettingsScreen**: 4 Inset Grouped sections (`IosListSection`, `IosListRow`) with squircle category icons, indented dividers, and `IosModalDialog`.
   - **EditorScreen**: Clean iOS navigation bar with Cancel/Done text actions, inline capsule date/time picker pill, and modal sheets.
   - **Secondary Screens**: Polish `CalendarScreen`, `OnThisDayScreen`, `SearchScreen`, `StatsScreen`, `TrashScreen` with iOS headers and spring touch physics.
3. **R3. Business Logic Preservation & Zero Regression (M6)**:
   - Maintain 100% functionality and zero regression across Room database DAOs, security/PIN cipher, and Supabase cloud sync.
   - Pass `./gradlew assembleDebug` and all unit test suites (`./gradlew test`).

## Acceptance Criteria
- `./gradlew test` and `./gradlew assembleDebug` compile successfully with 0 errors.
- Non-UI business domains (Room, Sync, Security) remain completely functional without regressions.
- No Android FAB or 3-dot overflow menus exist on primary user journeys.
- Primary navigation operates smoothly across the 4 root tabs via `IosTabBar`.
- Settings screen renders strictly as an Inset Grouped list with indented dividers and squircle icons.
- Timeline screen displays collapsible large title and segmented filters.

Maintain your `BRIEFING.md`, `plan.md`, and `progress.md` in your working directory.
When finished and verified, report completion back to the Sentinel.
