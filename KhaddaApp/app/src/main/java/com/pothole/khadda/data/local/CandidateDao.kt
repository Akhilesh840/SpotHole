package com.pothole.khadda.data.local

import androidx.room.*
import com.pothole.khadda.model.CandidateEvent
import com.pothole.khadda.model.CandidateStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CandidateEvent Entity.
 */
@Dao
interface CandidateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: CandidateEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidates(candidates: List<CandidateEvent>)

    @Update
    suspend fun updateCandidate(candidate: CandidateEvent)

    @Query("UPDATE candidate_events SET status = :status, maxConfidence = :maxConfidence, inferenceMsPerFrame = :inferenceMs WHERE id = :id")
    suspend fun updateCandidateStatus(id: String, status: CandidateStatus, maxConfidence: Float, inferenceMs: Long)

    @Query("SELECT * FROM candidate_events WHERE id = :id")
    suspend fun getCandidateById(id: String): CandidateEvent?

    @Query("SELECT * FROM candidate_events WHERE status = :status ORDER BY timestampMs DESC")
    suspend fun getCandidatesByStatus(status: CandidateStatus): List<CandidateEvent>

    @Query("SELECT * FROM candidate_events WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    suspend fun getCandidatesForSession(sessionId: String): List<CandidateEvent>

    @Query("SELECT * FROM candidate_events WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getCandidatesForSessionFlow(sessionId: String): Flow<List<CandidateEvent>>

    @Query("SELECT * FROM candidate_events WHERE status = 'PENDING' ORDER BY timestampMs ASC")
    suspend fun getPendingCandidates(): List<CandidateEvent>

    @Query("SELECT * FROM candidate_events WHERE sessionId = :sessionId AND status = 'PENDING' ORDER BY timestampMs ASC")
    suspend fun getPendingCandidatesForSession(sessionId: String): List<CandidateEvent>

    @Query("SELECT * FROM candidate_events WHERE status = 'PENDING' ORDER BY timestampMs ASC")
    fun getPendingCandidatesFlow(): Flow<List<CandidateEvent>>

    @Query("SELECT COUNT(*) FROM candidate_events WHERE sessionId = :sessionId")
    suspend fun getCandidateCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM candidate_events WHERE sessionId = :sessionId AND status = 'PENDING'")
    suspend fun getPendingCountForSession(sessionId: String): Int

    @Query("DELETE FROM candidate_events WHERE sessionId = :sessionId")
    suspend fun clearCandidatesForSession(sessionId: String)

    @Query("DELETE FROM candidate_events")
    suspend fun clearAllCandidates()
}
