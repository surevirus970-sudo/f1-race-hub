package com.f1racehub.app.domain.repository

import com.f1racehub.app.domain.model.GrandPrix
import kotlinx.coroutines.flow.Flow

interface RaceRepository {
    fun observeGrandPrixList(): Flow<List<GrandPrix>>
    suspend fun refreshSchedule(): Result<Unit>
    suspend fun updateSessionAlarm(sessionId: String, isSet: Boolean)
}
