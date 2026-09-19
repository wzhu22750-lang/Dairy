package com.example.inkpaperdiary.data.repository

import com.example.inkpaperdiary.core.database.AppDatabase
import com.example.inkpaperdiary.core.database.entity.toDomain
import com.example.inkpaperdiary.core.database.entity.toEntity
import com.example.inkpaperdiary.domain.model.DiaryDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 草稿存储契约。抽象出接口以便在 JVM 单元测试中用内存实现替换 Room。
 */
interface DraftStore {
    suspend fun saveDraft(draft: DiaryDraft)
    suspend fun getDraft(draftId: String): DiaryDraft?
    suspend fun getLatestNewDiaryDraft(): DiaryDraft?
    suspend fun getDraftForDiary(diaryId: String): DiaryDraft?
    suspend fun getAllDrafts(): List<DiaryDraft>
    suspend fun deleteDraft(draftId: String)
    suspend fun deleteDraftsForDiary(diaryId: String)
    /** 清理空草稿，返回清理数量 */
    suspend fun purgeEmptyDrafts(): Int
}

/**
 * 草稿仓库：编辑器未确认内容的唯一持久层出口。
 * 草稿与正式日记分表存储（drafts vs diary_entries），
 * 只有用户显式保存后才会由 DiaryRepository 落入正式数据。
 */
class DraftRepository(private val database: AppDatabase) : DraftStore {

    private val draftDao = database.draftDao()

    override suspend fun saveDraft(draft: DiaryDraft) = withContext(Dispatchers.IO) {
        draftDao.upsert(draft.toEntity())
    }

    override suspend fun getDraft(draftId: String): DiaryDraft? = withContext(Dispatchers.IO) {
        draftDao.getById(draftId)?.toDomain()
    }

    override suspend fun getLatestNewDiaryDraft(): DiaryDraft? = withContext(Dispatchers.IO) {
        draftDao.getNewDiaryDrafts().firstOrNull()?.toDomain()
    }

    override suspend fun getDraftForDiary(diaryId: String): DiaryDraft? = withContext(Dispatchers.IO) {
        draftDao.getByDiaryId(diaryId)?.toDomain()
    }

    override suspend fun getAllDrafts(): List<DiaryDraft> = withContext(Dispatchers.IO) {
        draftDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteDraft(draftId: String): Unit = withContext(Dispatchers.IO) {
        draftDao.deleteById(draftId)
    }

    override suspend fun deleteDraftsForDiary(diaryId: String): Unit = withContext(Dispatchers.IO) {
        draftDao.deleteByDiaryId(diaryId)
    }

    override suspend fun purgeEmptyDrafts(): Int = withContext(Dispatchers.IO) {
        val empty = draftDao.getAll().filter { it.toDomain().isBlank }
        empty.forEach { draftDao.deleteById(it.draftId) }
        empty.size
    }
}
