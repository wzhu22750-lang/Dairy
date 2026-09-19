package com.example.inkpaperdiary.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 日记草稿：编辑过程中的未确认数据，独立于正式 Diary 存储在 Room `drafts` 表中。
 *
 * - 新建日记的草稿：diaryId = null，draftId 即未来正式日记的 id
 * - 编辑既有日记的草稿：diaryId = 被编辑日记的 id，draftId 与其相同（草稿"遮蔽"正式数据，
 *   保存确认后删除草稿，从未确认前绝不写入 diary_entries）
 */
data class DiaryDraft(
    val draftId: String,
    val diaryId: String?,
    val userId: String,
    val title: String,
    val contentMarkdown: String,
    val weather: Weather = Weather.SUNNY,
    val locationName: String? = null,
    val entryDate: Long,
    /** 正式日记的 createdAt；新建草稿中记录 0，保存时由 Repository 落定 */
    val diaryCreatedAt: Long = 0L,
    val isPinned: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val createdTime: Long,
    val updatedTime: Long,
    val metadata: DraftMetadata = DraftMetadata()
) {
    /** 空草稿判定：标题、正文、配图、标签、地点全为空时不应持久化（与 Editor 的空日记语义一致） */
    val isBlank: Boolean
        get() = title.isBlank() &&
            contentMarkdown.isBlank() &&
            attachments.isEmpty() &&
            tags.isEmpty() &&
            locationName.isNullOrBlank()
}

/** 草稿附加元数据（与正文内容分离，避免污染 Diary 模型） */
@Serializable
data class DraftMetadata(
    /** "补写某天"入口携带的目标日期 */
    val initialEntryDate: Long? = null
)

@Serializable
data class DraftTagPayload(
    val id: String,
    val name: String,
    val colorHex: String = "#9E3323",
    val createdAt: Long = 0L
)

@Serializable
data class DraftAttachmentPayload(
    val id: String,
    val diaryId: String,
    val localPath: String,
    val remoteUrl: String? = null,
    val fileName: String,
    val fileSize: Long = 0L,
    val mimeType: String = "image/webp",
    val sortOrder: Int = 0
)

/**
 * 草稿列表字段的 JSON 编解码。
 * 解码失败（如 schema 演进）时降级为空列表，保证草稿主体（标题/正文）永不因附属字段损坏而丢失。
 */
object DraftJsonCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encodeTags(tags: List<Tag>): String = json.encodeToString(
        tags.map { DraftTagPayload(it.id, it.name, it.colorHex, it.createdAt) }
    )

    fun decodeTags(raw: String): List<Tag> = runCatching {
        json.decodeFromString<List<DraftTagPayload>>(raw)
            .map { Tag(id = it.id, name = it.name, colorHex = it.colorHex, createdAt = it.createdAt) }
    }.getOrDefault(emptyList())

    fun encodeAttachments(attachments: List<Attachment>): String = json.encodeToString(
        attachments.map {
            DraftAttachmentPayload(
                id = it.id,
                diaryId = it.diaryId,
                localPath = it.localPath,
                remoteUrl = it.remoteUrl,
                fileName = it.fileName,
                fileSize = it.fileSize,
                mimeType = it.mimeType,
                sortOrder = it.sortOrder
            )
        }
    )

    fun decodeAttachments(raw: String): List<Attachment> = runCatching {
        json.decodeFromString<List<DraftAttachmentPayload>>(raw)
            .map {
                Attachment(
                    id = it.id,
                    diaryId = it.diaryId,
                    localPath = it.localPath,
                    remoteUrl = it.remoteUrl,
                    fileName = it.fileName,
                    fileSize = it.fileSize,
                    mimeType = it.mimeType,
                    sortOrder = it.sortOrder
                )
            }
    }.getOrDefault(emptyList())

    fun encodeMetadata(metadata: DraftMetadata): String = json.encodeToString(metadata)

    fun decodeMetadata(raw: String): DraftMetadata = runCatching {
        json.decodeFromString<DraftMetadata>(raw)
    }.getOrDefault(DraftMetadata())
}
