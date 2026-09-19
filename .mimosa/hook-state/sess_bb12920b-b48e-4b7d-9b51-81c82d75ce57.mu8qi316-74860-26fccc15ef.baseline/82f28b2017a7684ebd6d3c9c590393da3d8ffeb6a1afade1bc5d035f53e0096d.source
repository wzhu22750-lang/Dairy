package com.example.inkpaperdiary.domain.model

data class Attachment(
    val id: String,
    val diaryId: String,
    val localPath: String,
    val remoteUrl: String? = null,
    val fileName: String,
    val fileSize: Long = 0L,
    val mimeType: String = "image/webp",
    val sortOrder: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.DIRTY
)
