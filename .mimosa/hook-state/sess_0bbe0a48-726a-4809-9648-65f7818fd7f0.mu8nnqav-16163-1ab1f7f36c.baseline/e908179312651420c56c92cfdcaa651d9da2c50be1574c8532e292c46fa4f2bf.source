package com.example.inkpaperdiary.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inkpaperdiary.core.database.dao.AttachmentDao
import com.example.inkpaperdiary.core.database.dao.DiaryDao
import com.example.inkpaperdiary.core.database.dao.TagDao
import com.example.inkpaperdiary.core.database.entity.AttachmentEntity
import com.example.inkpaperdiary.core.database.entity.DiaryEntity
import com.example.inkpaperdiary.core.database.entity.DiaryTagCrossRef
import com.example.inkpaperdiary.core.database.entity.TagEntity

@Database(
    entities = [
        DiaryEntity::class,
        AttachmentEntity::class,
        TagEntity::class,
        DiaryTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ink_paper_diary.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
