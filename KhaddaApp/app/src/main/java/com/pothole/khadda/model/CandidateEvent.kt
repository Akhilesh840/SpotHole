package com.pothole.khadda.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Status lifecycle of a candidate road anomaly event.
 */
enum class CandidateStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    ERROR
}

/**
 * CandidateEvent Entity representing a candidate pothole logged during a ride
 * awaiting post-ride visual verification.
 */
@Entity(
    tableName = "candidate_events",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["status"])
    ]
)
data class CandidateEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val triggeredZDiff: Boolean,
    val triggeredZThresh: Boolean,
    val triggeredStdev: Boolean,
    val triggeredGZero: Boolean,
    val zDiff: Double,
    val zThresh: Double,
    val stdev: Double,
    val impact: Double,
    val severity: SeverityLevel,
    var status: CandidateStatus = CandidateStatus.PENDING,
    var maxConfidence: Float = 0.0f,
    val framePaths: List<String> = emptyList(),
    var inferenceMsPerFrame: Long = 0L,
    val modelFileName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
