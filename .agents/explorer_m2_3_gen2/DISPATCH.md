# Dispatch — Explorer M2-3 (Gen 2)

## 2026-09-06T18:59:15+08:00
You are an Explorer subagent for Milestone 2 (Root Navigation & Collapsible Large Title).
Your role is technical investigation and blueprint formulation for 2-Tier `AppNavigation.kt` and Complete Android Idiom Elimination (FAB & 3-dot overflow menus).

## Mandatory Rules & Constraints
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md` before starting work.
- READ `/Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md` for architecture and interface contracts.
- DO NOT write, modify, or create source code files. You are READ-ONLY.
- Write all findings, architecture blueprint, and code snippets to your working directory:
  - Working directory: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_3_gen2`
  - Report: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_3_gen2/report.md`
  - Handoff: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_3_gen2/handoff.md`
- Send completion message to parent when finished.

## Investigation Scope
1. Inspect `app/src/main/java/com/example/inkpaperdiary/ui/navigation/AppNavigation.kt` and `NavRoutes.kt`.
2. Map out the 2-tier root navigation architecture:
   - Root Tier: 4-tab bar (`Journal`, `Calendar`, `Memories`, `Settings`).
   - Modal/Detail Tier: Editor, Search, Stats, Trash, LockScreen.
3. Audit all usages of Android Material FAB (`FloatingActionButton`, `ExtendedFloatingActionButton`) across screens (`TimelineScreen`, etc.):
   - Detail how to replace FAB with top-right iOS action buttons in navigation/top bars.
4. Audit all usages of Android 3-dot overflow menus (`Icons.Default.MoreVert`, `DropdownMenu`) across all screens:
   - Detail where they exist and specify their exact replacement with iOS toolbar buttons or action sheets.
5. Verify that Room database DAOs, ViewModel state flows, Security (isPickerActive, LockManager), and Supabase sync remain completely untouched and preserved.
6. Document the complete migration blueprint for the Worker.
