package com.f1racehub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity

@Database(
    entities = [RaceEntity::class, RaceSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class F1Database : RoomDatabase() {
    abstract fun raceDao(): RaceDao
}
