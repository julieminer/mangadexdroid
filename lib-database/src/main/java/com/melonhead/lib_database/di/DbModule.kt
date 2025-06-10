package com.melonhead.lib_database.di

import androidx.room.Room
import com.melonhead.lib_database.chapter.ChapterDBMigrations
import com.melonhead.lib_database.chapter.ChapterDatabase
import com.melonhead.lib_database.manga.MangaDBMigrations
import com.melonhead.lib_database.manga.MangaDatabase
import com.melonhead.lib_database.readmarkers.ReadMarkerDatabase
import com.melonhead.lib_database.sync_queue.SyncQueueDao
import com.melonhead.lib_database.sync_queue.SyncQueueDatabase
import com.melonhead.lib_database.sync_queue.SyncQueueEvent
import com.melonhead.lib_database.sync_queue.SyncQueueEventTypeConverters
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.dsl.module

val LibDbModule = module {
    single(createdAtStart = true) {
        Room.databaseBuilder(
            get(),
            ChapterDatabase::class.java, "chapter"
        ).addMigrations(
            ChapterDBMigrations.MIGRATION_1_2,
            ChapterDBMigrations.MIGRATION_2_3,
        ).build()
    }

    single(createdAtStart = true) {
        Room.databaseBuilder(
            get(),
            MangaDatabase::class.java, "manga"
        ).addMigrations(
            MangaDBMigrations.MIGRATION_1_2,
            MangaDBMigrations.MIGRATION_2_3,
            MangaDBMigrations.MIGRATION_3_4,
            MangaDBMigrations.MIGRATION_4_5,
            MangaDBMigrations.MIGRATION_5_6,
            MangaDBMigrations.MIGRATION_6_7,
        ).build()
    }

    single(createdAtStart = true) {
        Room.databaseBuilder(
            get(),
            ReadMarkerDatabase::class.java, "readmarker"
        ).build()
    }

    single(createdAtStart = true) {
        Room.databaseBuilder(
            get(),
            SyncQueueDatabase::class.java, SyncQueueDao.TABLE_NAME
        ).addTypeConverter(
            get<SyncQueueEventTypeConverters>()
        ).build()
    }

    single(createdAtStart = true) {
        Json {
            prettyPrint = false // Or true for debugging
            isLenient = true
            ignoreUnknownKeys = true // Good practice
            serializersModule = SerializersModule {
                polymorphic(SyncQueueEvent::class) {
                    subclass(SyncQueueEvent.MarkRead::class)
                    subclass(SyncQueueEvent.ChangeRating::class)
                    subclass(SyncQueueEvent.ChangeSeriesReadingStatus::class)
                    subclass(SyncQueueEvent.UpdateMangaReadingStatus::class)
                }
            }
        }
    }

    single(createdAtStart = true) {
        SyncQueueEventTypeConverters(get<Json>())
    }

    single {
        get<MangaDatabase>().mangaDao()
    }

    single {
        get<ChapterDatabase>().chapterDao()
    }

    single {
        get<ReadMarkerDatabase>().readMarkersDao()
    }

    single {
        get<SyncQueueDatabase>().syncQueueDao()
    }
}
