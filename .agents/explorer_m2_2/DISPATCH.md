## 2026-09-06T18:49:01+08:00
You are Explorer M2-2 (Milestone 2: IosLargeTitleScaffold Component).
Your working directory is: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2`
Project root: `/Users/kuangqie/Documents/VibeCoding/日记本`

Read:
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/ORIGINAL_REQUEST.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/PROJECT.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/spec_miner_survey_2/spec_requirements.md`
- `/Users/kuangqie/Documents/VibeCoding/日记本/TEST_READY.md`

Your mission:
Investigate and design `IosLargeTitleScaffold.kt` under `app/src/main/java/com/example/inkpaperdiary/core/designsystem/scaffold/`:
1. Dynamic scroll-coupled collapsible header:
   - 34sp Bold Large Title smoothly fading out as user scrolls past 52dp.
   - Centered 17sp SemiBold inline title fading in as large title collapses.
   - Frosted glass elevation (93% translucency via `AppleMaterials.barBackgroundColor()`) with 0.5dp bottom border when collapsed.
2. Layout parameters: `title`, `scrollState: ScrollState`, `navigationIcon`, `actions`, `content: @Composable (PaddingValues) -> Unit`.
3. Support both `ScrollState` and `LazyListState` (or nested scroll connection).
4. Output your design and implementation blueprint to `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m2_2/report.md` and `handoff.md`.
Send message back when complete.
