# Plan — Project Orchestrator (Generation 2)

## Overall Objective
Complete the Apple Human Interface Guidelines (HIG) architectural refactoring of `com.example.inkpaperdiary` from Milestone 2 through Milestone 6, fulfilling all remaining requirements (R1, R2, R3).

## Milestone Roadmap
1. **Milestone 2: Root Navigation Architecture & Collapsible Large Title**
   - Deliverables:
     - `app/src/main/java/com/example/inkpaperdiary/ui/navigation/IosTabBar.kt`: 4-tab bar (Journal, Calendar, Memories, Settings) with 93% translucency, 0.5dp specular hairline border, active/inactive Cupertino icons and tint, spring tap feedback.
     - `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`: 34sp Bold Large Title transitioning to centered 17sp SemiBold inline title with frosted glass elevation at 52dp scroll offset.
     - `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt`: 2-tier navigation structure (Root 4-tab bar + modal push for Editor, Search, Stats, Trash, Lock).
     - Elimination of Android FABs and 3-dot overflow menus (`Icons.Default.MoreVert`).
   - Loop: 3 Explorers -> 1 Worker -> 2 Reviewers -> 2 Challengers -> 1 Forensic Auditor -> Gate.

2. **Milestone 3: Timeline Screen Overhaul**
   - Apple Journal-style stream with segmented control filters, spring-press cards, top-right compose action, contextual action sheets.
   - Loop: 3 Explorers -> 1 Worker -> 2 Reviewers -> 2 Challengers -> 1 Forensic Auditor -> Gate.

3. **Milestone 4: Settings Screen & Modal Sheets/Dialogs**
   - 4 Inset Grouped sections (`IosListSection`, `IosListRow`) with squircle category icons, indented dividers, and `IosModalDialog` / `IosActionSheet`.
   - Loop: 3 Explorers -> 1 Worker -> 2 Reviewers -> 2 Challengers -> 1 Forensic Auditor -> Gate.

4. **Milestone 5: Editor & Secondary Screens Polish**
   - `EditorScreen` with Cancel/Done text actions, inline capsule date/time picker pill, and `IosDateTimePickerSheet`.
   - `CalendarScreen`, `OnThisDayScreen`, `SearchScreen`, `StatsScreen`, `TrashScreen` with iOS headers and spring touch physics.
   - Loop: 3 Explorers -> 1 Worker -> 2 Reviewers -> 2 Challengers -> 1 Forensic Auditor -> Gate.

5. **Milestone 6: Final Verification & Coverage Hardening**
   - Non-UI business domain preservation (Room DAOs, Security cipher, Supabase sync).
   - 100% pass of all 152+ test cases, clean `assembleDebug` with 0 errors.
   - Phase 2 Adversarial coverage hardening (Challengers + Worker + Reviewers).
   - Final comprehensive Forensic Audit.

## Succession Rule
- Track spawn count. If count >= 16 and all subagents completed, execute succession protocol.
