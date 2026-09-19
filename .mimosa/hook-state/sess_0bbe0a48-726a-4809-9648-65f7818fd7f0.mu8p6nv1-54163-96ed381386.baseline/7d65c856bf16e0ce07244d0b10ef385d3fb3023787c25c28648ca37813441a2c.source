package com.example.inkpaperdiary.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [
        Index(value = ["entryDate"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isPinned"]),
        Index(value = ["syncStatus"])
    ]
)
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "local_guest") val userId: String = "local_guest",
    val title: String = "",
    val contentMarkdown: String = "",
    val mood: String = "CALM",
    val weather: String = "SUNNY",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: Int = 1 // 0: SYNCED, 1: DIRTY, 2: DELETED
)
