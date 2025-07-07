package com.melonhead.lib_database

import androidx.room.TypeConverter
import kotlin.time.Instant

internal object InstantConverter {
    @TypeConverter
    fun toInstant(dateMillis: Long): Instant {
        return Instant.fromEpochMilliseconds(dateMillis)
    }

    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }
}
