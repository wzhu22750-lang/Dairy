## 2026-09-06T18:27:20+08:00
You are Spec Miner 2 (Survey: iOS HIG & Component Requirements Specification).
Your working directory is: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/spec_miner_survey_2`
Project root: `/Users/kuangqie/Documents/VibeCoding/日记本`
Read `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`.

Your mission:
Formulate an authoritative, exact specification and component contract for R1, R2, and R3:
1. R1: iOS Design System & Interaction Primitives:
   - Materials & Vibrancy: Ultra-thin, thin, regular, thick, ultra-thick translucent materials; 0.5dp hairline specular gradient border (`AppleMaterials.glassBorder`).
   - iOS Touch Physics (`iosClick`): Spring scale-down (0.96~0.98x), alpha dimming (0.85x), haptic feedback (`LocalHapticFeedback`). No Material ink ripple.
   - Inset Grouped List components: `IosListSection`, `IosListRow`, `IosNavigationRow`, `IosSwitchRow` with 14~16dp squircle corners, 30dp squircle icon box, 56dp indented 0.5dp divider.
   - iOS Segmented Control: Pill slider with animated indicator.
2. R2: Root Navigation Architecture & Collapsible Large Title:
   - Bottom Translucent Tab Bar (`IosTabBar`): 4 tabs (`Journal`, `Calendar`, `Memories`, `Settings`), 93% translucency, hairline top border.
   - Dynamic Collapsible Large Title: 34sp Bold Large Title transitioning to centered 17sp SemiBold inline title with frosted glass elevation.
   - Complete removal of Android FAB and 3-dot overflow menu (`Icons.Default.MoreVert`).
3. R3: Screen Layout & Component Overhaul:
   - TimelineScreen: Apple Journal-style stream, segmented control filter, spring-press diary cards, contextual long-press actions, compose action in navigation bar/toolbar.
   - SettingsScreen: Inset Grouped list with `IosListSection` and `IosListRow`.
   - EditorScreen & Dialogs: iOS-style toolbar, iOS modal action sheets replacing Android alert dialogs, date/time pickers.
4. Define exact Kotlin/Compose function signatures, parameters, layout behaviors, visual values (colors, alpha, radii, paddings).
5. Write your findings to `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/spec_miner_survey_2/spec_requirements.md` and provide a structured `handoff.md`.
