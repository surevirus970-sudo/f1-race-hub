package com.f1racehub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity
import com.f1racehub.app.data.local.entities.RaceWithSessions
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaces(races: List<RaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<RaceSessionEntity>)

    @Transaction
    @Query("SELECT * FROM races ORDER BY round ASC")
    fun observeRacesWithSessions(): Flow<List<RaceWithSessions>>

    @Query("UPDATE race_sessions SET isAlarmSet = :isSet WHERE sessionId = :sessionId")
    suspend fun updateAlarmStatus(sessionId: String, isSet: Boolean)

    @Query("SELECT * FROM race_sessions WHERE isAlarmSet = 1")
    suspend fun getScheduledAlarms(): List<RaceSessionEntity>
}
