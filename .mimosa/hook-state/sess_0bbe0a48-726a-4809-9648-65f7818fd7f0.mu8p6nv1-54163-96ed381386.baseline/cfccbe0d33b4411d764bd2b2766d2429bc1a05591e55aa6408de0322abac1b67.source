package com.example.inkpaperdiary.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inkpaperdiary.core.database.entity.DraftEntity

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE draftId = :draftId LIMIT 1")
    suspend fun getById(draftId: String): DraftEntity?

    /** 新建日记的草稿（diaryId IS NULL），按最近更新排序 */
    @Query("SELECT * FROM drafts WHERE diaryId IS NULL ORDER BY updatedTime DESC")
    suspend fun getNewDiaryDrafts(): List<DraftEntity>

    /** 既有日记的编辑草稿 */
    @Query("SELECT * FROM drafts WHERE diaryId = :diaryId ORDER BY updatedTime DESC LIMIT 1")
    suspend fun getByDiaryId(diaryId: String): DraftEntity?

    @Query("SELECT * FROM drafts ORDER BY updatedTime DESC")
    suspend fun getAll(): List<DraftEntity>

    @Query("DELETE FROM drafts WHERE draftId = :draftId")
    suspend fun deleteById(draftId: String)

    @Query("DELETE FROM drafts WHERE diaryId = :diaryId")
    suspend fun deleteByDiaryId(diaryId: String)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
}
