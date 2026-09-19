package com.example.inkpaperdiary.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.inkpaperdiary.domain.model.Attachment
import com.example.inkpaperdiary.domain.model.DiaryDraft
import com.example.inkpaperdiary.domain.model.DraftJsonCodec
import com.example.inkpaperdiary.domain.model.DraftMetadata
import com.example.inkpaperdiary.domain.model.Tag
import com.example.inkpaperdiary.domain.model.Weather

/**
 * 编辑器草稿表。独立于 diary_entries：草稿在用户显式保存（"完成"）前绝不写入正式数据，
 * 保证任何异常退出（返回、后台被杀、Activity 重建、进程死亡）后都能恢复到最近一次输入。
 */
@Entity(
    tableName = "drafts",
    indices = [Index(value = ["diaryId"])]
)
data class DraftEntity(
    @PrimaryKey val draftId: String,
    /** null = 为新建日记产生的草稿；非空 = 对既有日记的编辑草稿 */
    val diaryId: String?,
    val userId: String,
    val title: String,
    val contentMarkdown: String,
    val weather: String,
    val locationName: String?,
    val entryDate: Long,
    /** 正式日记的 createdAt，编辑保存时回填，避免创建时间被重置 */
    val diaryCreatedAt: Long,
    val isPinned: Boolean,
    val tagsJson: String,
    val attachmentsJson: String,
    val createdTime: Long,
    val updatedTime: Long,
    val metadataJson: String
)

fun DraftEntity.toDomain(): DiaryDraft = DiaryDraft(
    draftId = draftId,
    diaryId = diaryId,
    userId = userId,
    title = title,
    contentMarkdown = contentMarkdown,
    weather = Weather.fromCode(weather),
    locationName = locationName,
    entryDate = entryDate,
    diaryCreatedAt = diaryCreatedAt,
    isPinned = isPinned,
    tags = DraftJsonCodec.decodeTags(tagsJson),
    attachments = DraftJsonCodec.decodeAttachments(attachmentsJson),
    createdTime = createdTime,
    updatedTime = updatedTime,
    metadata = DraftJsonCodec.decodeMetadata(metadataJson)
)

fun DiaryDraft.toEntity(): DraftEntity = DraftEntity(
    draftId = draftId,
    diaryId = diaryId,
    userId = userId,
    title = title,
    contentMarkdown = contentMarkdown,
    weather = weather.code,
    locationName = locationName,
    entryDate = entryDate,
    diaryCreatedAt = diaryCreatedAt,
    isPinned = isPinned,
    tagsJson = DraftJsonCodec.encodeTags(tags),
    attachmentsJson = DraftJsonCodec.encodeAttachments(attachments),
    createdTime = createdTime,
    updatedTime = updatedTime,
    metadataJson = DraftJsonCodec.encodeMetadata(metadata)
)
