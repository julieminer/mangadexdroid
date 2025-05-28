package com.melonhead.lib_database.chapter

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object ChapterDBMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {

        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chapter ADD COLUMN blockedChapter INTEGER NOT NULL DEFAULT(0)")
        }
    }
}