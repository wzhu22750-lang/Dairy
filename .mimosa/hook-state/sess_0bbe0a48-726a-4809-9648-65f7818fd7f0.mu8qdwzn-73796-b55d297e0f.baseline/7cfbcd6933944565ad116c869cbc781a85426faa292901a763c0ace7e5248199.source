package com.example.inkpaperdiary.core.database.dao

import androidx.room.*
import com.example.inkpaperdiary.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM diary_attachments WHERE diaryId = :diaryId ORDER BY sortOrder ASC")
    fun getAttachmentsForDiary(diaryId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM diary_attachments WHERE diaryId = :diaryId")
    suspend fun deleteForDiary(diaryId: String)

    @Query("DELETE FROM diary_attachments WHERE id = :id")
    suspend fun deleteById(id: String)
}
