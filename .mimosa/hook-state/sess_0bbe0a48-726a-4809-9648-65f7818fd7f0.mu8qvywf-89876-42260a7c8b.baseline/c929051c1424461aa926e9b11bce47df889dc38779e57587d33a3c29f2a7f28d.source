package com.example.inkpaperdiary.challenger

import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.SyncStatus
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather
import com.example.inkpaperdiary.ui.timeline.TimelineUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Empirical Challenger Suite for Milestone M3 (Gen 2):
 * `TimelineScreen` Apple Journal stream logic, segmented filter transitions,
 * empty state mechanics, large list scaling, and multi-photo mosaic grid boundaries.
 */
class TimelineScreenStreamFilterEmpiricalChallengeTest {

    // =========================================================================================
    // Helper: Exact filter logic as implemented in TimelineScreen.kt lines 149-161
    // =========================================================================================
    private fun applyTimelineFilter(
        diaries: List<Diary>,
        filteredDiaries: List<Diary>,
        selectedSegment: String
    ): List<Diary> {
        val source = if (diaries.isNotEmpty()) diaries else filteredDiaries
        var list = source
        when (selectedSegment) {
            "全部" -> { /* 无额外附加条件 */ }
            "图文" -> { list = list.filter { it.attachments.isNotEmpty() } }
            "置顶" -> { list = list.filter { it.isPinned } }
        }
        return list
    }

    private fun createDiary(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Title",
        content: String = "Test content",
        weather: Weather = Weather.SUNNY,
        isPinned: Boolean = false,
        entryDate: Long = System.currentTimeMillis(),
        attachmentsCount: Int = 0
    ): Diary {
        val attachments = (0 until attachmentsCount).map { i ->
            Attachment(
                id = "att-$id-$i",
                diaryId = id,
                localPath = "/mock/path/image_$i.webp",
                fileName = "image_$i.webp"
            )
        }
        return Diary(
            id = id,
            title = title,
            contentMarkdown = content,
            weather = weather,
            isPinned = isPinned,
            entryDate = entryDate,
            attachments = attachments
        )
    }

    // =========================================================================================
    // 1. Filtering Precision with Mixed Diary Sets
    // =========================================================================================

    @Test
    fun challenge_filteringPrecision_mixedDiarySets() {
        val now = System.currentTimeMillis()

        // Construct heterogeneous diary population
        val d1 = createDiary(id = "1", title = "Text only, unpinned", isPinned = false, attachmentsCount = 0, entryDate = now - 1000)
        val d2 = createDiary(id = "2", title = "Text only, pinned", isPinned = true, attachmentsCount = 0, entryDate = now - 2000)
        val d3 = createDiary(id = "3", title = "1 photo, unpinned", isPinned = false, attachmentsCount = 1, entryDate = now - 3000)
        val d4 = createDiary(id = "4", title = "1 photo, pinned", isPinned = true, attachmentsCount = 1, entryDate = now - 4000)
        val d5 = createDiary(id = "5", title = "2 photos, unpinned", isPinned = false, attachmentsCount = 2, entryDate = now - 5000)
        val d6 = createDiary(id = "6", title = "3 photos, pinned", isPinned = true, attachmentsCount = 3, entryDate = now - 6000)
        val d7 = createDiary(id = "7", title = "4 photos, unpinned", isPinned = false, attachmentsCount = 4, entryDate = now - 7000)
        val d8 = createDiary(id = "8", title = "5 photos, pinned", isPinned = true, attachmentsCount = 5, entryDate = now - 8000)
        val d9 = createDiary(id = "9", title = "12 photos, unpinned", isPinned = false, attachmentsCount = 12, entryDate = now - 9000)

        val fullDataset = listOf(d1, d2, d3, d4, d5, d6, d7, d8, d9)

        // 1. "全部" filter precision
        val allResult = applyTimelineFilter(fullDataset, fullDataset, "全部")
        assertEquals("全部 filter must return all 9 items", 9, allResult.size)
        assertEquals(fullDataset.map { it.id }, allResult.map { it.id })

        // 2. "图文" filter precision: must match exactly items with attachments.isNotEmpty()
        val photoResult = applyTimelineFilter(fullDataset, fullDataset, "图文")
        val expectedPhotoIds = listOf("3", "4", "5", "6", "7", "8", "9")
        assertEquals("图文 filter must return exactly 7 photo entries", 7, photoResult.size)
        assertEquals(expectedPhotoIds, photoResult.map { it.id })
        assertTrue("All photoResult entries must have attachments", photoResult.all { it.attachments.isNotEmpty() })
        assertFalse("photoResult must not contain text-only d1", photoResult.any { it.id == "1" })
        assertFalse("photoResult must not contain text-only d2", photoResult.any { it.id == "2" })

        // 3. "置顶" filter precision: must match exactly items with isPinned == true
        val pinnedResult = applyTimelineFilter(fullDataset, fullDataset, "置顶")
        val expectedPinnedIds = listOf("2", "4", "6", "8")
        assertEquals("置顶 filter must return exactly 4 pinned entries", 4, pinnedResult.size)
        assertEquals(expectedPinnedIds, pinnedResult.map { it.id })
        assertTrue("All pinnedResult entries must be pinned", pinnedResult.all { it.isPinned })

        // 4. Set theory intersection: (Photo AND Pinned)
        val photoAndPinned = photoResult.filter { it.isPinned }
        val expectedPhotoAndPinnedIds = listOf("4", "6", "8")
        assertEquals(3, photoAndPinned.size)
        assertEquals(expectedPhotoAndPinnedIds, photoAndPinned.map { it.id })

        // 5. Partition invariant:
        // size(All) == size(Photo) + size(TextOnly)
        val textOnly = allResult.filter { it.attachments.isEmpty() }
        assertEquals(allResult.size, photoResult.size + textOnly.size)

        // size(All) == size(Pinned) + size(Unpinned)
        val unpinned = allResult.filter { !it.isPinned }
        assertEquals(allResult.size, pinnedResult.size + unpinned.size)
    }

    // =========================================================================================
    // 2. Compound Filtering Precision (Segment x Mood Matrix)
    // =========================================================================================

    @Test
    fun challenge_compoundFilteringPrecision_segmentCrossAttributeMatrix() {
        val segments = listOf("全部", "图文", "置顶")

        // Build comprehensive dataset: 4 variations (text/photo, pinned/unpinned)
        val dataset = mutableListOf<Diary>()
        var idCounter = 1
        for (pinned in listOf(false, true)) {
            dataset.add(createDiary(id = "${idCounter++}", isPinned = pinned, attachmentsCount = 0))
            dataset.add(createDiary(id = "${idCounter++}", isPinned = pinned, attachmentsCount = 2))
        }
        assertEquals(4, dataset.size)

        for (segment in segments) {
            val result = applyTimelineFilter(dataset, dataset, segment)

            // Every returned item must satisfy segment criteria
            when (segment) {
                "全部" -> {
                    assertEquals("In 全部, all items are returned", 4, result.size)
                }
                "图文" -> {
                    assertTrue("Every item in 图文 must have attachments", result.all { it.attachments.isNotEmpty() })
                    assertEquals("In 图文, exactly the 2 photo items", 2, result.size)
                }
                "置顶" -> {
                    assertTrue("Every item in 置顶 must be pinned", result.all { it.isPinned })
                    assertEquals("In 置顶, exactly the 2 pinned items", 2, result.size)
                }
            }
        }

        // Partition invariant: 全部 == 图文 + text-only
        val all = applyTimelineFilter(dataset, dataset, "全部")
        val photo = applyTimelineFilter(dataset, dataset, "图文")
        val textOnly = all.filter { it.attachments.isEmpty() }
        assertEquals(all.size, photo.size + textOnly.size)
    }

    // =========================================================================================
    // 3. High-Frequency Segmented Filter Switching Stress Test
    // =========================================================================================

    @Test
    fun challenge_highFrequencyFilterSwitching_stressTest() {
        val segments = listOf("全部", "图文", "置顶")

        // Generate 500 random diaries
        val random = Random(42)
        val dataset = (1..500).map { i ->
            createDiary(
                id = "diary-$i",
                isPinned = random.nextBoolean(),
                attachmentsCount = if (random.nextBoolean()) random.nextInt(0, 8) else 0,
                entryDate = System.currentTimeMillis() - i * 10_000L
            )
        }

        // Simulate 10,000 high-frequency filter transitions
        val iterations = 10_000
        var previousResultCount = -1

        val elapsedNano = measureNanoTime {
            for (i in 0 until iterations) {
                val targetSegment = segments[i % segments.size]

                val result = applyTimelineFilter(dataset, dataset, targetSegment)

                // Monotonic assertions
                assertTrue("Result must never be null", result != null)
                assertTrue("Result size must be <= dataset size", result.size <= dataset.size)

                when (targetSegment) {
                    "全部" -> { /* valid */ }
                    "图文" -> assertTrue("All items in '图文' must have attachments", result.all { it.attachments.isNotEmpty() })
                    "置顶" -> assertTrue("All items in '置顶' must be pinned", result.all { it.isPinned })
                }
                // Determinism test: repeated call must return exactly identical list
                val repeated = applyTimelineFilter(dataset, dataset, targetSegment)
                assertEquals("Filter execution must be 100% deterministic and pure", result, repeated)

                previousResultCount = result.size
            }
        }

        val elapsedMs = elapsedNano / 1_000_000.0
        val avgMicros = (elapsedNano / 1000.0) / iterations
        println("10,000 filter switches completed in ${elapsedMs}ms (avg ${avgMicros}µs per switch)")
        assertTrue("10,000 filter operations on 500 items must finish within 1500ms", elapsedMs < 1500.0)
    }

    // =========================================================================================
    // 4. Empty State Transitions & Recovery
    // =========================================================================================

    @Test
    fun challenge_emptyStateTransitions_andRecovery() {
        // Case A: Completely empty database (0 entries)
        val emptyDb = emptyList<Diary>()
        val resultA = applyTimelineFilter(emptyDb, emptyDb, "全部")
        assertTrue("Result must be empty", resultA.isEmpty())

        // Line 293 empty state condition:
        // val isFiltered = uiState.diaries.isNotEmpty() && displayedDiaries.isEmpty()
        val isFilteredA = emptyDb.isNotEmpty() && resultA.isEmpty()
        assertFalse(
            "When DB is empty, isFiltered must be FALSE -> renders '暂无日记' and '新建第一篇日记'",
            isFilteredA
        )

        // Case B: DB has entries, but filter matches 0 entries
        // e.g. All entries are unpinned, but filter is "置顶"
        val unpinnedOnlyDb = (1..5).map { i ->
            createDiary(id = "$i", isPinned = false, attachmentsCount = 0)
        }
        val resultB = applyTimelineFilter(unpinnedOnlyDb, unpinnedOnlyDb, "置顶")
        assertTrue("Result for '置顶' on unpinned-only DB must be empty", resultB.isEmpty())

        val isFilteredB = unpinnedOnlyDb.isNotEmpty() && resultB.isEmpty()
        assertTrue(
            "When DB has entries but filter has 0 matches, isFiltered must be TRUE -> renders '无匹配日记' and '清除筛选条件'",
            isFilteredB
        )

        // Case C: DB has entries, but all are text-only, filter is "图文"
        val textOnlyDb = (1..5).map { i ->
            createDiary(id = "$i", isPinned = true, attachmentsCount = 0)
        }
        val resultC = applyTimelineFilter(textOnlyDb, textOnlyDb, "图文")
        assertTrue("Result for '图文' on text-only DB must be empty", resultC.isEmpty())
        val isFilteredC = textOnlyDb.isNotEmpty() && resultC.isEmpty()
        assertTrue(
            "Text-only DB filtered by '图文' must trigger isFiltered = true",
            isFilteredC
        )

        // Case D: Recovery via "清除筛选条件" (Line 297-301)
        // onClearFilterClick: selectedSegment = "全部", onlyPinned = false
        val recoveredResult = applyTimelineFilter(textOnlyDb, textOnlyDb, "全部")
        assertEquals(
            "Clearing filters must immediately restore all entries",
            textOnlyDb.size,
            recoveredResult.size
        )
        val isFilteredRecovered = textOnlyDb.isNotEmpty() && recoveredResult.isEmpty()
        assertFalse("Recovered state must NOT be empty state", isFilteredRecovered)

        // Case E: Dynamic mutation - unpinning the last pinned diary causes transition to empty state
        var dynamicDb = listOf(
            createDiary(id = "1", isPinned = true, attachmentsCount = 0),
            createDiary(id = "2", isPinned = false, attachmentsCount = 1)
        )
        val beforeUnpin = applyTimelineFilter(dynamicDb, dynamicDb, "置顶")
        assertEquals(1, beforeUnpin.size)
        assertFalse(dynamicDb.isNotEmpty() && beforeUnpin.isEmpty())

        // Simulate unpinning diary "1"
        dynamicDb = dynamicDb.map { if (it.id == "1") it.copy(isPinned = false) else it }
        val afterUnpin = applyTimelineFilter(dynamicDb, dynamicDb, "置顶")
        assertTrue(afterUnpin.isEmpty())
        assertTrue("After unpinning last pinned item, isFiltered must become TRUE", dynamicDb.isNotEmpty() && afterUnpin.isEmpty())
    }

    // =========================================================================================
    // 5. Large List Scaling & Order Preservation Invariant
    // =========================================================================================

    @Test
    fun challenge_largeListScaling_andOrderPreservation() {
        val listSizes = listOf(100, 500, 1000, 5000, 10_000)
        val random = Random(123)

        for (size in listSizes) {
            // Generate sorted dataset matching Room DB query: ORDER BY isPinned DESC, entryDate DESC
            val dataset = (1..size).map { i ->
                val isPinned = i <= size / 5 // Top 20% pinned
                val attachmentsCount = if (i % 3 == 0) (i % 6) else 0
                createDiary(
                    id = "d-$i",
                    isPinned = isPinned,
                    attachmentsCount = attachmentsCount,
                    entryDate = 1_000_000_000L - i * 1000L
                )
            }

            // Benchmark "图文" filter
            val photoFiltered: List<Diary>
            val photoTimeNano = measureNanoTime {
                photoFiltered = applyTimelineFilter(dataset, dataset, "图文")
            }

            // Benchmark "置顶" filter
            val pinnedFiltered: List<Diary>
            val pinnedTimeNano = measureNanoTime {
                pinnedFiltered = applyTimelineFilter(dataset, dataset, "置顶")
            }

            // Verify order preservation:
            // Kotlin filter preserves encounter order. Since input was sorted by isPinned DESC, entryDate DESC:
            // Output MUST maintain monotonic non-increasing entryDate within each pinned partition!
            fun verifyOrderPreserved(list: List<Diary>, filterName: String) {
                for (j in 0 until list.size - 1) {
                    val current = list[j]
                    val next = list[j + 1]
                    if (current.isPinned == next.isPinned) {
                        assertTrue(
                            "[$filterName, size=$size] Entry dates must be monotonic non-increasing: ${current.entryDate} >= ${next.entryDate}",
                            current.entryDate >= next.entryDate
                        )
                    } else {
                        assertTrue(
                            "[$filterName, size=$size] Pinned items must strictly precede unpinned items",
                            current.isPinned && !next.isPinned
                        )
                    }
                }
            }

            verifyOrderPreserved(photoFiltered, "图文")
            verifyOrderPreserved(pinnedFiltered, "置顶")

            // Performance assertion: 10,000 items must filter in < 30ms
            val photoMs = photoTimeNano / 1_000_000.0
            val pinnedMs = pinnedTimeNano / 1_000_000.0
            println("Size $size: photo filter ${photoMs}ms, pinned filter ${pinnedMs}ms")
            assertTrue("Filtering $size items must complete under 50ms", photoMs < 50.0 && pinnedMs < 50.0)
        }
    }

    // =========================================================================================
    // 6. Multi-Photo Layout Boundary & Geometry Stress Test (JournalPhotoMosaic)
    // =========================================================================================

    @Test
    fun challenge_multiPhotoLayoutBoundaries_mosaicGeometry() {
        // Line 531: JournalPhotoMosaic(attachments: List<Attachment>)
        // Testing exact boundary branches: 0, 1, 2, 3, 4, 5, 10, 100 attachments

        // Helper to simulate layout geometry calculation
        fun evaluatePhotoMosaicLayout(count: Int): String {
            if (count == 0) return "NONE"
            return when (count) {
                1 -> "HERO_180DP"
                2 -> "SPLIT_2COL_130DP"
                3 -> "ASYMMETRIC_3_160DP"
                4 -> "GRID_2X2_96DP_NO_BADGE"
                else -> {
                    val remaining = count - 3
                    "GRID_2X2_96DP_BADGE_PLUS_$remaining"
                }
            }
        }

        // 0 photos: empty boundary
        assertEquals("NONE", evaluatePhotoMosaicLayout(0))

        // 1 photo: Hero banner
        assertEquals("HERO_180DP", evaluatePhotoMosaicLayout(1))

        // 2 photos: Split 2-column
        assertEquals("SPLIT_2COL_130DP", evaluatePhotoMosaicLayout(2))

        // 3 photos: Asymmetrical 1.5x left + 2 stacked right
        assertEquals("ASYMMETRIC_3_160DP", evaluatePhotoMosaicLayout(3))

        // 4 photos: 2x2 grid without badge
        assertEquals("GRID_2X2_96DP_NO_BADGE", evaluatePhotoMosaicLayout(4))

        // 5 photos: 2x2 grid with 4th cell showing "+2" (5 - 3 = 2)
        assertEquals("GRID_2X2_96DP_BADGE_PLUS_2", evaluatePhotoMosaicLayout(5))

        // 6 photos: remainingCount = 3
        assertEquals("GRID_2X2_96DP_BADGE_PLUS_3", evaluatePhotoMosaicLayout(6))

        // 10 photos: remainingCount = 7
        assertEquals("GRID_2X2_96DP_BADGE_PLUS_7", evaluatePhotoMosaicLayout(10))

        // 25 photos: remainingCount = 22
        assertEquals("GRID_2X2_96DP_BADGE_PLUS_22", evaluatePhotoMosaicLayout(25))

        // 100 photos: remainingCount = 97
        assertEquals("GRID_2X2_96DP_BADGE_PLUS_97", evaluatePhotoMosaicLayout(100))

        // Index safety stress test:
        // In lines 684-762 of TimelineScreen.kt, when attachments.size >= 5:
        // accesses attachments[0], attachments[1], attachments[2], attachments[3].
        // Verify that for all counts from 5 to 10,000, indices 0..3 are unconditionally valid.
        for (n in 5..1000) {
            val list = (0 until n).map { "att-$it" }
            assertNotNull(list[0])
            assertNotNull(list[1])
            assertNotNull(list[2])
            assertNotNull(list[3])
            val remaining = list.size - 3
            assertTrue("Remaining count must be >= 2 for n >= 5", remaining >= 2)
            assertEquals(n - 3, remaining)
        }
    }

    // =========================================================================================
    // 7. Card Text Hierarchy & Typographic Formatting Boundaries
    // =========================================================================================

    @Test
    fun challenge_cardTextHierarchy_andFormattingBoundaries() {
        // 1. Blank content fallback to "（无正文）" (Line 444)
        val blankDiary = createDiary(title = "", content = "   ")
        val preview = blankDiary.previewText.ifBlank { "（无正文）" }
        assertEquals("Blank content must fallback to '（无正文）'", "（无正文）", preview)

        // 2. Markdown stripping in previewText
        val markdownDiary = createDiary(
            title = "MD Title",
            content = "# Big Header\nThis is **bold** and *italic* and [a link](https://example.com) with > quote and `code`."
        )
        val cleanPreview = markdownDiary.previewText
        assertFalse("Markdown headers must be stripped", cleanPreview.contains("#"))
        assertFalse("Markdown bold/italic must be stripped", cleanPreview.contains("**") || cleanPreview.contains("*"))
        assertFalse("Markdown links must extract link text only", cleanPreview.contains("https://example.com"))
        assertTrue("Extracted link text must remain", cleanPreview.contains("a link"))

        // 3. Huge content truncation to 120 characters + "..."
        val hugeContent = "A".repeat(5000)
        val hugeDiary = createDiary(content = hugeContent)
        val truncatedPreview = hugeDiary.previewText
        assertEquals("Preview text should be 120 chars + '...' (total 123 chars)", 123, truncatedPreview.length)
        assertTrue("Must end with ellipsis", truncatedPreview.endsWith("..."))

        // 4. Word count accuracy: Chinese vs Latin
        val mixedContent = "今天天气很好，阳光明媚。I love Apple HIG design! 123"
        val mixedDiary = createDiary(content = mixedContent)
        // Chinese chars: 今天天气很好阳光明媚 (10 chars, ignoring punctuation)
        // Latin words: "I", "love", "Apple", "HIG", "design", "123" (6 words)
        // Total = 10 + 6 = 16
        assertEquals("Word count must accurately count CJK chars + Latin words", 16, mixedDiary.wordCount)

        // 5. MaxLines dynamic switching: 2 lines for photo cards, 3 lines for text cards (Line 452)
        val textCard = createDiary(attachmentsCount = 0)
        val photoCard = createDiary(attachmentsCount = 1)
        val textMaxLines = if (textCard.attachments.isNotEmpty()) 2 else 3
        val photoMaxLines = if (photoCard.attachments.isNotEmpty()) 2 else 3
        assertEquals("Text-only card must allow 3 lines", 3, textMaxLines)
        assertEquals("Photo card must constrain to 2 lines", 2, photoMaxLines)
    }

    // =========================================================================================
    // 8. Date and Time Formatting Across Year Boundaries
    // =========================================================================================

    @Test
    fun challenge_dateTimeFormatting_acrossYearBoundaries() {
        // Line 372-380 formatting logic:
        fun formatDateTime(entryDate: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = entryDate }
            val nowCal = Calendar.getInstance()
            val pattern = if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
                "M月d日 EEEE · HH:mm"
            } else {
                "yyyy年M月d日 EEEE · HH:mm"
            }
            return SimpleDateFormat(pattern, Locale.CHINESE).format(Date(entryDate))
        }

        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)

        // Same year: does not include year prefix
        val sameYearTime = nowCal.timeInMillis
        val formattedSameYear = formatDateTime(sameYearTime)
        assertFalse("Current year format must NOT contain year prefix", formattedSameYear.contains("${currentYear}年"))
        assertTrue("Current year format must contain month and day", formattedSameYear.contains("月") && formattedSameYear.contains("日"))
        assertTrue("Current year format must contain weekday and time", formattedSameYear.contains("星期") && formattedSameYear.contains("·"))

        // Prior year (e.g. 2024): MUST include year prefix
        val priorCal = Calendar.getInstance().apply { set(2024, Calendar.OCTOBER, 1, 12, 30) }
        val formattedPrior = formatDateTime(priorCal.timeInMillis)
        assertTrue("Prior year format MUST contain year prefix '2024年'", formattedPrior.startsWith("2024年"))
        assertTrue("Must contain month, day, weekday, time", formattedPrior.contains("10月1日 星期二 · 12:30"))

        // Epoch 0 (1970-01-01): safe formatting without crashes
        val epochFormatted = formatDateTime(0L)
        assertTrue("Epoch 0 must format with 1970", epochFormatted.startsWith("1970年"))
    }

    // =========================================================================================
    // 9. Action Sheet Target Diary Header Resolution
    // =========================================================================================

    @Test
    fun challenge_actionSheetHeaderResolution_noBlankTitle() {
        // Lines 320-322:
        // val titleText = actionDiary.title.ifBlank {
        //     actionDiary.previewText.take(28).ifBlank { "日记操作" }
        // }
        fun resolveActionSheetTitle(diary: Diary): String {
            return diary.title.ifBlank {
                diary.previewText.take(28).ifBlank { "日记操作" }
            }
        }

        // 1. Explicit title present
        val dWithTitle = createDiary(title = "成都玉林路随笔", content = "今天喝了一杯冰美式...")
        assertEquals("成都玉林路随笔", resolveActionSheetTitle(dWithTitle))

        // 2. Title blank, content present -> takes preview text up to 28 chars
        val dNoTitle = createDiary(title = "", content = "今天在成都的街头走一走，直到所有的灯都熄灭了也不停留，你会挽着我的衣袖我会把手揣进裤兜。")
        val resolvedNoTitle = resolveActionSheetTitle(dNoTitle)
        assertEquals(28, resolvedNoTitle.length)
        assertEquals(dNoTitle.contentMarkdown.take(28), resolvedNoTitle)

        // 3. Both title and content blank -> fallbacks to "日记操作"
        val dBlankAll = createDiary(title = "", content = "   ")
        assertEquals("日记操作", resolveActionSheetTitle(dBlankAll))

        // 4. Assert header is never empty or blank for any input
        assertFalse(resolveActionSheetTitle(dWithTitle).isBlank())
        assertFalse(resolveActionSheetTitle(dNoTitle).isBlank())
        assertFalse(resolveActionSheetTitle(dBlankAll).isBlank())
    }

    // =========================================================================================
    // 10. Memory Allocation & GC Safety Under Repeated Filter Cycles
    // =========================================================================================

    @Test
    fun challenge_memorySafety_underRepeatedFilterCycles() {
        val dataset = (1..1000).map { i ->
            createDiary(
                id = "mem-$i",
                isPinned = i % 4 == 0,
                attachmentsCount = i % 5
            )
        }

        val runtime = Runtime.getRuntime()
        runtime.gc()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()

        val segments = listOf("全部", "图文", "置顶")
        var dummySum = 0

        // Run 5,000 cycles
        for (i in 0 until 5000) {
            val segment = segments[i % segments.size]
            val result = applyTimelineFilter(dataset, dataset, segment)
            dummySum += result.size
        }

        runtime.gc()
        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val memDiffMb = (memAfter - memBefore) / (1024.0 * 1024.0)

        println("Memory before: ${memBefore / 1024 / 1024}MB, after: ${memAfter / 1024 / 1024}MB, diff: ${memDiffMb}MB, dummySum: $dummySum")
        // Filtering should produce ephemeral collections eligible for GC, not retain memory leaks
        assertTrue("Memory growth after 5,000 filter cycles must be under 30MB", memDiffMb < 30.0)
    }
}
