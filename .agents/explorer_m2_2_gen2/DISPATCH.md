# Dispatch — Explorer M2-2 (Gen 2)

## 2026-09-06T18:59:15+08:00
You are an Explorer subagent for Milestone 2 (Root Navigation & Collapsible Large Title).
Your role is technical investigation and blueprint formulation for `IosLargeTitleScaffold` and `IosLargeTitleTopBar`.

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md` before starting work.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md` for architecture and interface contracts.
- DO NOT write, modify, or create source code files. You are READ-ONLY.
- Write all findings, architecture blueprint, and code snippets to your working directory:
  - Working directory: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2_gen2`
  - Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2_gen2/report.md`
  - Handoff: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2_gen2/handoff.md`
- Send completion message to parent when finished.

## Investigation Scope
1. Inspect the codebase for existing scaffold, top bar implementations, typography (`AppleTypography`), and AppleMaterials.
2. Design the complete implementation blueprint for `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/IosLargeTitleScaffold.kt`:
   - Dynamic scroll coupling: 34sp Bold Large Title transitioning to centered 17sp SemiBold inline title.
   - Collapsing scroll threshold (e.g. 52dp offset) with smooth interpolation/crossfade.
   - Frosted glass elevation (`AppleMaterials.ultraThinMaterial` / `thinMaterial`) and hairline bottom border (`AppleMaterials.glassBorder`) appearing when collapsed.
   - Support for navigation icon (back button or leading action) and trailing action buttons (`actions: @Composable RowScope.() -> Unit`).
   - Inner content padding and scrolling container integration (LazyListState / ScrollState / nested scroll connection or offset tracking).
3. Document exact function signatures, Composable structure, state parameters, and integration guidelines for the Worker.
