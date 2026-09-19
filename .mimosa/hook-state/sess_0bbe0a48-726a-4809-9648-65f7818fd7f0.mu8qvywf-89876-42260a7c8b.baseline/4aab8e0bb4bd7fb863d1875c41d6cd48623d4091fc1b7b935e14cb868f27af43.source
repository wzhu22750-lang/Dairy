package com.example.inkpaperdiary.draft

import com.example.inkpaperdiary.core.database.entity.DraftEntity
import com.example.inkpaperdiary.core.database.entity.toDomain
import com.example.inkpaperdiary.core.database.entity.toEntity
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.DiaryDraft
import com.example.inkpaperdiary.domain.model.DraftJsonCodec
import com.example.inkpaperdiary.domain.model.DraftMetadata
import com.example.inkpaperdiary.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 草稿模型层测试：JSON 编解码、空草稿判定、实体映射。
 * 这些纯逻辑是草稿可靠性的地基：任何字段损坏都不能让正文丢失。
 */
class DraftModelTest {

    private fun sampleDraft() = DiaryDraft(
        draftId = "draft-1",
        diaryId = null,
        userId = "local_guest",
        title = "雨天书桌",
        contentMarkdown = "# 雨\n\n窗外下着雨，**很安静**。\n- 咖啡\n- 书",
        weather = com.example.inkpaperdiary.domain.model.Weather.RAINY,
        locationName = "书房",
        entryDate = 1_700_000_000_000L,
        diaryCreatedAt = 0L,
        isPinned = false,
        tags = listOf(Tag(id = "t1", name = "随笔"), Tag(id = "t2", name = "生活")),
        attachments = listOf(
            Attachment(
                id = "a1", diaryId = "draft-1", localPath = "/data/media/img_a1.webp",
                fileName = "img_a1.webp", fileSize = 1024, sortOrder = 0
            )
        ),
        createdTime = 1_700_000_000_000L,
        updatedTime = 1_700_000_060_000L,
        metadata = DraftMetadata(initialEntryDate = 1_699_900_000_000L)
    )

    @Test
    fun `标签JSON编解码往返无损`() {
        val tags = listOf(Tag(id = "t1", name = "随笔"), Tag(id = "t2", name = "生活"))
        val decoded = DraftJsonCodec.decodeTags(DraftJsonCodec.encodeTags(tags))
        assertEquals(tags, decoded)
    }

    @Test
    fun `附件JSON编解码往返无损`() {
        val attachments = listOf(
            Attachment(id = "a1", diaryId = "d1", localPath = "/x/a.webp", fileName = "a.webp", fileSize = 9),
            Attachment(id = "a2", diaryId = "d1", localPath = "/x/b.webp", remoteUrl = "https://r/b", fileName = "b.webp", sortOrder = 1)
        )
        val decoded = DraftJsonCodec.decodeAttachments(DraftJsonCodec.encodeAttachments(attachments))
        assertEquals(attachments, decoded)
    }

    @Test
    fun `损坏的JSON解码降级为空集合而不抛异常`() {
        assertTrue(DraftJsonCodec.decodeTags("not-json{").isEmpty())
        assertTrue(DraftJsonCodec.decodeAttachments("}}{").isEmpty())
        assertEquals(DraftMetadata(), DraftJsonCodec.decodeMetadata("???"))
    }

    @Test
    fun `草稿实体映射往返无损`() {
        val draft = sampleDraft()
        val roundTrip = draft.toEntity().toDomain()
        assertEquals(draft, roundTrip)
    }

    @Test
    fun `空草稿判定覆盖标题正文配图标签地点`() {
        val base = sampleDraft()
        assertTrue(
            base.copy(title = "", contentMarkdown = "", attachments = emptyList(), tags = emptyList(), locationName = null).isBlank
        )
        assertTrue(
            base.copy(title = "  ", contentMarkdown = " ", attachments = emptyList(), tags = emptyList(), locationName = "").isBlank
        )
        // 任何一项非空都不是空草稿
        assertNotEquals(true, base.copy(title = "", contentMarkdown = "x").isBlank)
        assertNotEquals(true, base.copy(title = "", contentMarkdown = "", attachments = listOf(base.attachments.first())).isBlank)
        assertNotEquals(true, base.copy(title = "", contentMarkdown = "", tags = listOf(Tag(id = "t", name = "x"))).isBlank)
        assertNotEquals(true, base.copy(title = "", contentMarkdown = "", locationName = "家").isBlank)
    }

    @Test
    fun `元数据JSON往返无损`() {
        val metadata = DraftMetadata(initialEntryDate = 123L)
        assertEquals(metadata, DraftJsonCodec.decodeMetadata(DraftJsonCodec.encodeMetadata(metadata)))
        assertEquals(DraftMetadata(), DraftJsonCodec.decodeMetadata(DraftJsonCodec.encodeMetadata(DraftMetadata())))
    }

    @Test
    fun `实体默认字段与迁移建表语句字段一致`() {
        // DraftEntity 的列集合必须与 MIGRATION_1_2 的 CREATE TABLE 保持一致（Room 启动时校验 schema）
        val entity = DraftEntity(
            draftId = "d", diaryId = null, userId = "u", title = "", contentMarkdown = "",
            weather = "SUNNY", locationName = null, entryDate = 0L, diaryCreatedAt = 0L,
            isPinned = false, tagsJson = "[]", attachmentsJson = "[]",
            createdTime = 0L, updatedTime = 0L, metadataJson = "{}"
        )
        val declaredColumns = DraftEntity::class.java.declaredFields
            .map { it.name }
            .filterNot { it == "serialVersionUID" || it == "\$stable" }
            .toSet()
        assertEquals(
            "实体字段与迁移 SQL 列不一致",
            setOf(
                "draftId", "diaryId", "userId", "title", "contentMarkdown", "weather",
                "locationName", "entryDate", "diaryCreatedAt", "isPinned",
                "tagsJson", "attachmentsJson", "createdTime", "updatedTime", "metadataJson"
            ),
            declaredColumns
        )
        assertEquals("d", entity.draftId)
    }
}
