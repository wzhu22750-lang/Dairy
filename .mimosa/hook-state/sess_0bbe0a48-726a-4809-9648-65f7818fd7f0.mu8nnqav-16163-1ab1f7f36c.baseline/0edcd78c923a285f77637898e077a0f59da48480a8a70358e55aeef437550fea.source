package com.example.inkpaperdiary.core.database.dao

import androidx.room.*
import com.example.inkpaperdiary.core.database.entity.DiaryEntity
import com.example.inkpaperdiary.core.database.entity.DiaryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 ORDER BY isPinned DESC, entryDate DESC")
    fun getAllDiaries(): Flow<List<DiaryWithDetails>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id AND isDeleted = 0")
    suspend fun getDiaryById(id: String): DiaryWithDetails?

    /** 不论是否软删除都返回，供同步等场景判断记录是否存在。 */
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getDiaryEntityAnyStatus(id: String): DiaryEntity?

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 AND entryDate >= :startTime AND entryDate <= :endTime ORDER BY entryDate ASC")
    fun getDiariesBetweenDates(startTime: Long, endTime: Long): Flow<List<DiaryWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM diary_entries 
        WHERE isDeleted = 0 
        AND (title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' OR locationName LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, entryDate DESC
    """)
    fun searchDiaries(query: String): Flow<List<DiaryWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM diary_entries 
        WHERE isDeleted = 0 
        AND strftime('%m-%d', datetime(entryDate / 1000, 'unixepoch', 'localtime')) = :monthDay
        ORDER BY entryDate DESC
    """)
    suspend fun getOnThisDay(monthDay: String): List<DiaryWithDetails>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashDiaries(): Flow<List<DiaryWithDetails>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE syncStatus != 0")
    suspend fun getUnsyncedDiaries(): List<DiaryWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(diary: DiaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diaries: List<DiaryEntity>)

    @Query("UPDATE diary_entries SET isDeleted = 1, deletedAt = :deletedAt, syncStatus = 2 WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE diary_entries SET isDeleted = 0, deletedAt = NULL, syncStatus = 1 WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM diary_entries WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun purgeOldTrash(cutoffTime: Long)

    @Query("UPDATE diary_entries SET syncStatus = :newStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, newStatus: Int)

    @Query("UPDATE diary_entries SET isPinned = :pinned, syncStatus = 1 WHERE id = :id")
    suspend fun updatePinned(id: String, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE isDeleted = 0")
    fun getDiaryCount(): Flow<Int>
}