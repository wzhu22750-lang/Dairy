package com.example.inkpaperdiary.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_attachments",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["diaryId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val diaryId: String,
    val localPath: String,
    val remoteUrl: String? = null,
    val fileName: String,
    val fileSize: Long = 0L,
    val mimeType: String = "image/webp",
    val sortOrder: Int = 0,
    val syncStatus: Int = 1 // 0: SYNCED, 1: DIRTY, 2: DELETED
)
