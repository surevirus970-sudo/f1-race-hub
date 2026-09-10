package com.f1racehub.app.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "races")
data class RaceEntity(
    @PrimaryKey val round: Int,
    val raceName: String,
    val circuitId: String,
    val circuitName: String,
    val country: String,
    val locality: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "race_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RaceEntity::class,
            parentColumns = ["round"],
            childColumns = ["round"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("round")]
)
data class RaceSessionEntity(
    @PrimaryKey val sessionId: String, // e.g. "1_RACE"
    val round: Int,
    val sessionType: String, // FP1, FP2, FP3, QUALIFYING, SPRINT, RACE
    val startTimeUtc: String,
    val isAlarmSet: Boolean = false
)

data class RaceWithSessions(
    @Embedded val race: RaceEntity,
    @Relation(
        parentColumn = "round",
        entityColumn = "round"
    )
    val sessions: List<RaceSessionEntity>
)
