package com.example.inkpaperdiary

import com.example.inkpaperdiary.domain.model.Diary
import com.example.inkpaperdiary.domain.model.SyncStatus
import com.example.inkpaperdiary.domain.model.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryModelTest {

    @Test
    fun testWordCountAndPreviewText() {
        val markdown = """
            # 今日随笔
            今天在**春熙路**喝了咖啡，天气很棒！
            - 喝了拿铁
            - 读了 20 页书
        """.trimIndent()

        val diary = Diary(
            title = "悠闲午后",
            contentMarkdown = markdown,
            weather = Weather.SUNNY
        )

        // 中文按字、英文/数字按词计数：中文字符数 + 英文单词数（in/coffee 等）
        val cjkCount = markdown.count { it.code in 0x3400..0x4DBF || it.code in 0x4E00..0x9FFF || it.code in 0xF900..0xFAFF || it.code in 0x3040..0x30FF || it.code in 0xAC00..0xD7AF }
        assertTrue(diary.wordCount >= cjkCount)
        assertTrue(diary.wordCount < markdown.length) // 标点/空白不再计入
        assertTrue(diary.previewText.contains("今日随笔"))
        assertTrue(diary.previewText.contains("今天在春熙路喝了咖啡"))
    }

    @Test
    fun testMoodAndWeatherFallback() {
        assertEquals(Weather.RAINY, Weather.fromCode("RAINY"))
        assertEquals(Weather.SUNNY, Weather.fromCode("unknown_weather"))

        assertEquals(SyncStatus.SYNCED, SyncStatus.fromCode(0))
        assertEquals(SyncStatus.DIRTY, SyncStatus.fromCode(1))
        assertEquals(SyncStatus.DELETED, SyncStatus.fromCode(2))
    }
}
