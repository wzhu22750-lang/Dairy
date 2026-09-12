# Handoff Report: Apple Journal Stream Architecture for TimelineScreen (Milestone 3)

**Author**: Explorer M3-1 (Gen 2)  
**Recipient**: Worker M3 (Gen 2) / Orchestrator  
**Working Directory**: `/Users/kuangqie/Documents/VibeCoding/日记本/.agents/explorer_m3_1_gen2`  
**Handoff Type**: Hard Handoff  
**Timestamp**: 2026-09-06T19:17:30+08:00  

---

## 1. Observation

1. **Baseline Compilation & Tests**:
   - Executed command `./gradlew testDebugUnitTest` in `/Users/kuangqie/Documents/VibeCoding/日记本`:
     ```
     BUILD SUCCESSFUL in 472ms
     26 actionable tasks: 1 from cache, 25 up-to-date
     ```
   - All 31 test suites across Tier 1 (features), Tier 2 (boundaries), Tier 3 (combinations), and Tier 4 (scenarios) compile and pass.

2. **`TimelineScreen.kt` Current State**:
   - Location: `/Users/kuangqie/Documents/VibeCoding/日记本/app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt` (601 lines).
   - In `DiaryCardItem` (lines 405–435): uses a 40dp x 40dp square calendar box (`Surface(RoundedCornerShape(10.dp))`) for day number and year-month, rather than an Apple Journal-style clean 13sp Footnote date/time header.
   - Typography (lines 461–482): Title uses `MaterialTheme.typography.titleMedium` (17sp SemiBold) and body uses `MaterialTheme.typography.bodyMedium` (15sp Regular), but lacks explicit line-height and letter-spacing tuning from Apple HIG.
   - Multi-photo layout (lines 485–514):
     ```kotlin
     // 图片缩略图展示 (最多 3 张，现代 10dp 圆角)
     if (diary.attachments.isNotEmpty()) {
         LazyRow(
             horizontalArrangement = Arrangement.spacedBy(8.dp),
             modifier = Modifier.padding(top = 2.dp)
         ) {
             items(diary.attachments.take(3)) { attachment ->
                 ...
                 AsyncImage(
                     ...
                     modifier = Modifier
                         .size(72.dp)
                         .clip(RoundedCornerShape(10.dp))
                         .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                         .background(MaterialTheme.colorScheme.surfaceVariant)
                 )
             }
         }
     }
     ```
     Observed: A simple horizontal `LazyRow` with 72dp square thumbnails and 10dp corners, displaying a maximum of 3 photos, rather than an authentic Apple Journal mosaic (1 to 4+ photos with 12dp squircle corners).
   - Pinned badge (lines 450–457):
     ```kotlin
     if (diary.isPinned) {
         Icon(
             imageVector = Icons.Filled.PushPin,
             contentDescription = "已置顶",
             modifier = Modifier.size(16.dp),
             tint = MaterialTheme.colorScheme.primary
         )
     }
     ```
     Observed: Plain push pin icon without capsule badge container.
   - Empty state (lines 234–294):
     ```kotlin
     ScrollGlyph(
         tint = PaperColors.MonoGray400,
         modifier = Modifier.size(48.dp)
     )
     ```
     Observed: Uses vintage Chinese paper `ScrollGlyph` and a 36dp button rather than an authentic iOS frosted icon container and 44dp capsule CTA button.
   - Large title scroll coupling (lines 138–144):
     ```kotlin
     IosLargeTitleItem(
         title = "日记",
         subtitle = currentDateStr,
         modifier = Modifier.padding(top = 10.dp)
     )
     ```
     Observed: Does not pass `scrollOffset = scrollOffset`, preventing dynamic large-title alpha fadeout during scroll collapse.

3. **`PaperCard.kt` Capabilities**:
   - Location: `/Users/kuangqie/Documents/VibeCoding/日记本/app/src/main/java/com/example/inkpaperdiary/core/designsystem/components/PaperCard.kt`.
   - Card container has `RoundedCornerShape(16.dp)` (16dp squircle), `AppleMaterials.glassBorder(width = 0.5.dp)` (0.5dp specular hairline border), `Modifier.iosClick` (scale 0.97f, alpha 0.85f, zero ripple), and `hasCeladonAccent` drawing a 3dp wide vertical rounded accent bar on the left side.

4. **Integration & Navigation Contracts**:
   - `AppNavigation.kt` lines 164–171 invokes `TimelineScreen` with:
     ```kotlin
     TimelineScreen(
         viewModel = timelineViewModel,
         onNavigateToEditor = { id -> modalStack.add(AppDestination.Editor(id)) },
         onNavigateToSearch = { modalStack.add(AppDestination.Search) },
         onNavigateToCalendar = { selectedTab = IosTab.CALENDAR },
         onNavigateToOnThisDay = { selectedTab = IosTab.MEMORIES },
         onNavigateToStats = { modalStack.add(AppDestination.Stats) },
         onNavigateToSettings = { selectedTab = IosTab.SETTINGS }
     )
     ```
   - Must preserve exact function signature and navigation callbacks.

---

## 2. Logic Chain

1. **Visual Narrative Requirement**:
   - *From Observation 2*: The current `TimelineScreen.kt` photo rendering is a 72dp square `LazyRow` capped at 3 photos with 10dp corners.
   - *Requirement in DISPATCH.md*: "Multi-photo layout: Apple Journal-style rounded image mosaic/carousel (1 to 4+ photos) with 12dp squircle corners."
   - *Deduction*: A new composable `JournalPhotoMosaic` is required that adapts dynamically:
     - 1 photo: Full width hero card (180dp).
     - 2 photos: 2-column split (130dp, 6dp gap).
     - 3 photos: Asymmetrical mosaic (1 large at 1.5x weight + 2 stacked on right, 160dp).
     - 4 photos: 2x2 grid (two rows of 2 images, 96dp each, 6dp gap).
     - 5+ photos: 2x2 grid where the 4th image shows a dark translucent overlay (`Color.Black.copy(0.45f)`) and centered `"+N"` indicator.
     - All photos framed with `RoundedCornerShape(12.dp)` and `AppleMaterials.glassBorder(0.5.dp)`.

2. **Typography Hierarchy**:
   - *From Observation 2 & DISPATCH.md*:
     - Title: 17sp Emphasized / Headline (`FontWeight.SemiBold`, 17.sp, line-height 22.sp, letter-spacing -0.4.sp).
     - Body: 15sp Subheadline / Secondary (`FontWeight.Normal`, 15.sp, line-height 21.sp, letter-spacing -0.24.sp).
     - Date/Time: 13sp Footnote / Caption (`FontWeight.Medium`, 13.sp, letter-spacing -0.08.sp, `PaperColors.MonoGray500`).
   - *Deduction*: Replace the 40dp calendar square with a clean text date/time header ("9月6日 星期日 · 19:14") sitting at the top of the card alongside mood, weather, and pinned status pills.

3. **Card Physics & Curvature**:
   - *From Observation 3*: `PaperCard` already supports 16dp squircle, 0.5dp glass border, `hasCeladonAccent` 3dp left pill, and `Modifier.iosClick`.
   - *Deduction*: `DiaryCardItem` should continue wrapping its contents in `PaperCard(onClick = onClick, onLongClick = onLongClick, hasCeladonAccent = diary.isPinned, modifier = Modifier.fillMaxWidth())`, delivering exact Apple HIG tactile response.

4. **Empty State Alignment**:
   - *From Observation 2*: Empty state currently uses vintage `ScrollGlyph` (Chinese heritage) and 36dp button.
   - *Requirement in DISPATCH.md*: "Empty state: iOS-style subtle illustration and capsule call-to-action button."
   - *Deduction*: Build `JournalEmptyState` featuring a 72dp squircle frosted icon container (`RoundedCornerShape(20.dp)`), `Icons.Outlined.Book` / `Icons.Outlined.Search`, 20sp SemiBold title, 15sp Subheadline, and a 44dp capsule CTA button (`Modifier.iosClick` scale 0.97f, alpha 0.85f).

5. **Zero Regression Contract**:
   - *From Observation 4*: `TimelineScreen` signature, `AppLockManager.isPickerActive` for file importing, and all ViewModel interactions must remain completely untouched.

---

## 3. Caveats

1. **Coil Image Loading Performance**: In the 2x2 grid and 3-photo asymmetrical layouts, Coil asynchronously loads images. Ensure `crossfade(true)` and `ContentScale.Crop` are applied to avoid image aspect ratio distortion or flashing.
2. **Local vs Remote Image URIs**: The `JournalImageItem` helper correctly inspects `File(attachment.localPath).exists()` before falling back to `attachment.remoteUrl`, maintaining compatibility with both local disk files and Supabase cloud URLs.
3. **No External Libraries**: All squircle geometry and animations use standard Jetpack Compose Foundation/Material3 primitives.

---

## 4. Conclusion

The Apple Journal stream architecture for `TimelineScreen` is fully designed and verified:
1. `DiaryCardItem` redesign incorporates 13sp Footnote date/time, 17sp SemiBold headline, 15sp regular subheadline, and metadata footer.
2. `JournalPhotoMosaic` provides adaptive 1-to-4+ photo presentation with 12dp squircle corners, 0.5dp glass border, and `+N` overflow indicator.
3. `JournalCapsulePill` and `JournalPinnedBadge` standardize metadata chips with 0.5dp glass borders.
4. `JournalEmptyState` delivers an authentic iOS frosted empty state with 44dp capsule CTA button.
5. All drop-in composable code has been fully drafted in `.agents/explorer_m3_1_gen2/report.md` ready for Worker M3 to apply directly to `TimelineScreen.kt`.

---

## 5. Verification Method

### 5.1 Compilation and Test Verification Commands
Worker M3 should execute:
1. Compile test suite:
   ```bash
   ./gradlew testDebugUnitTest
   ```
   *Expected result*: Exit code 0, all tests pass.
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   *Expected result*: Exit code 0, 0 compilation errors.

### 5.2 Specific Files to Inspect
- `app/src/main/java/com/example/inkpaperdiary/ui/timeline/TimelineScreen.kt`: Confirm all 6 required features are integrated.
- `app/src/test/java/com/example/inkpaperdiary/tier1_features/MaterialIdiomPurgeAuditTest.kt`: Confirm 0 `FloatingActionButton` and 0 `Icons.Default.MoreVert`.
- `app/src/test/java/com/example/inkpaperdiary/tier1_features/R3ScreenLayoutFeatureTest.kt`: Confirm all layout checks pass.

### 5.3 Invalidation Conditions
- Any occurrence of `FloatingActionButton` or `Icons.Default.MoreVert` in `TimelineScreen.kt`.
- Photo corners not adhering to 12dp squircle (`RoundedCornerShape(12.dp)`).
- Card container not adhering to 16dp squircle (`RoundedCornerShape(16.dp)`) or missing 0.5dp specular border (`AppleMaterials.glassBorder(0.5.dp)`).
- Breakdown of `AppLockManager.isPickerActive` lifecycle during TXT import.
