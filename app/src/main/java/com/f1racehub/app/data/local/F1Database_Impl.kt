package com.f1racehub.app.data.local

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.f1racehub.app.data.local.dao.RaceDao
import com.f1racehub.app.data.local.entities.RaceEntity
import com.f1racehub.app.data.local.entities.RaceSessionEntity
import com.f1racehub.app.data.local.entities.RaceWithSessions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class F1Database_Impl : F1Database() {

    private val raceDaoInstance: RaceDao = object : RaceDao {
        private val racesMap = ConcurrentHashMap<Int, RaceEntity>()
        private val sessionsMap = ConcurrentHashMap<String, RaceSessionEntity>()
        private val _flow = MutableStateFlow<List<RaceWithSessions>>(emptyList())

        private fun updateFlow() {
            val sortedRaces = racesMap.values.sortedBy { it.round }
            val list = sortedRaces.map { race ->
                val sessions = sessionsMap.values
                    .filter { it.round == race.round }
                    .sortedBy { it.startTimeUtc }
                RaceWithSessions(race = race, sessions = sessions)
            }
            _flow.value = list
        }

        override suspend fun insertRaces(races: List<RaceEntity>) {
            races.forEach { racesMap[it.round] = it }
            updateFlow()
        }

        override suspend fun insertSessions(sessions: List<RaceSessionEntity>) {
            sessions.forEach { sessionsMap[it.sessionId] = it }
            updateFlow()
        }

        override fun observeRacesWithSessions(): Flow<List<RaceWithSessions>> {
            return _flow.asStateFlow()
        }

        override suspend fun updateAlarmStatus(sessionId: String, isSet: Boolean) {
            val existing = sessionsMap[sessionId]
            if (existing != null) {
                sessionsMap[sessionId] = existing.copy(isAlarmSet = isSet)
                updateFlow()
            }
        }

        override suspend fun getScheduledAlarms(): List<RaceSessionEntity> {
            return sessionsMap.values.filter { it.isAlarmSet }
        }
    }

    override fun raceDao(): RaceDao = raceDaoInstance

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this, "races", "race_sessions")
    }

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        return config.sqliteOpenHelperFactory.create(
            SupportSQLiteOpenHelper.Configuration.builder(config.context)
                .name(config.name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) {}
                })
                .build()
        )
    }

    override fun clearAllTables() {}
}
