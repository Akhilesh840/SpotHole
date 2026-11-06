package com.pothole.khadda.data.repository

import com.pothole.khadda.data.local.CandidateDao
import com.pothole.khadda.data.local.PotholeDao
import com.pothole.khadda.data.local.SessionDao
import com.pothole.khadda.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

class PotholeRepository(
    private val potholeDao: PotholeDao,
    private val sessionDao: SessionDao,
    private val candidateDao: CandidateDao? = null
) {
    val allEventsFlow: Flow<List<PotholeEvent>> = potholeDao.getAllEventsFlow()
    val allSessionsFlow: Flow<List<DetectionSession>> = sessionDao.getAllSessionsFlow()

    suspend fun insertEvent(event: PotholeEvent) {
        potholeDao.insertEvent(event)
    }

    suspend fun insertSession(session: DetectionSession) {
        sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: DetectionSession) {
        sessionDao.updateSession(session)
    }

    suspend fun closeSession(sessionId: String, totalEvents: Int) {
        sessionDao.closeSession(sessionId, SessionStatus.STOPPED, System.currentTimeMillis(), totalEvents)
    }

    suspend fun updateRepairStatus(eventId: String, status: RepairStatus) {
        potholeDao.updateRepairStatus(eventId, status)
    }

    suspend fun deleteEvent(eventId: String) {
        potholeDao.deleteEventById(eventId)
    }

    suspend fun clearAll() {
        potholeDao.clearAllEvents()
        sessionDao.clearAllSessions()
        candidateDao?.clearAllCandidates()
    }

    suspend fun getAllEvents(): List<PotholeEvent> {
        return potholeDao.getAllEvents()
    }

    suspend fun getEventsForSession(sessionId: String): List<PotholeEvent> {
        return potholeDao.getEventsBySession(sessionId)
    }

    // Candidate Event Operations
    suspend fun insertCandidate(candidate: CandidateEvent) {
        candidateDao?.insertCandidate(candidate)
    }

    suspend fun updateCandidateStatus(id: String, status: CandidateStatus, maxConfidence: Float, inferenceMs: Long) {
        candidateDao?.updateCandidateStatus(id, status, maxConfidence, inferenceMs)
    }

    suspend fun getCandidateById(id: String): CandidateEvent? {
        return candidateDao?.getCandidateById(id)
    }

    suspend fun getCandidatesForSession(sessionId: String): List<CandidateEvent> {
        return candidateDao?.getCandidatesForSession(sessionId) ?: emptyList()
    }

    fun getCandidatesForSessionFlow(sessionId: String): Flow<List<CandidateEvent>> {
        return candidateDao?.getCandidatesForSessionFlow(sessionId) ?: emptyFlow()
    }

    suspend fun getPendingCandidates(): List<CandidateEvent> {
        return candidateDao?.getPendingCandidates() ?: emptyList()
    }

    suspend fun getPendingCandidatesForSession(sessionId: String): List<CandidateEvent> {
        return candidateDao?.getPendingCandidatesForSession(sessionId) ?: emptyList()
    }

    fun getPendingCandidatesFlow(): Flow<List<CandidateEvent>> {
        return candidateDao?.getPendingCandidatesFlow() ?: emptyFlow()
    }

    suspend fun getCandidateCountForSession(sessionId: String): Int {
        return candidateDao?.getCandidateCountForSession(sessionId) ?: 0
    }

    suspend fun getCandidatesByStatus(status: CandidateStatus): List<CandidateEvent> {
        return candidateDao?.getCandidatesByStatus(status) ?: emptyList()
    }

    suspend fun generateReport(sessionId: String): DetectionReport {
        val events = potholeDao.getEventsBySession(sessionId)
        val session = sessionDao.getSessionById(sessionId)

        var low = 0
        var medium = 0
        var high = 0
        val algoCounts = mutableMapOf<String, Int>()

        for (e in events) {
            when (e.severity) {
                SeverityLevel.LOW -> low++
                SeverityLevel.MEDIUM -> medium++
                SeverityLevel.HIGH -> high++
            }
            algoCounts[e.algorithm] = (algoCounts[e.algorithm] ?: 0) + 1
        }

        val duration = session?.getDuration() ?: 0L

        return DetectionReport(
            reportId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            totalEvents = events.size,
            lowCount = low,
            mediumCount = medium,
            highCount = high,
            generatedOn = System.currentTimeMillis(),
            durationMillis = duration,
            algorithmBreakdown = algoCounts
        )
    }
}
