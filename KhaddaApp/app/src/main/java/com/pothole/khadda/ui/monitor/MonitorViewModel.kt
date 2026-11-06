package com.pothole.khadda.ui.monitor

import android.app.Application
import androidx.lifecycle.*
import com.pothole.khadda.KhaddaApplication
import com.pothole.khadda.model.DetectionSession
import com.pothole.khadda.model.PotholeEvent
import com.pothole.khadda.model.SensorData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KhaddaApplication).repository

    val allEvents = repository.allEventsFlow.asLiveData()
    val allSessions = repository.allSessionsFlow.asLiveData()

    private val _isMonitoring = MutableLiveData(false)
    val isMonitoring: LiveData<Boolean> = _isMonitoring

    private val _isDemoMode = MutableLiveData(false)
    val isDemoMode: LiveData<Boolean> = _isDemoMode

    private val _currentSpeedKmh = MutableLiveData(0.0f)
    val currentSpeedKmh: LiveData<Float> = _currentSpeedKmh

    private val _liveGpsCoords = MutableLiveData(Pair(0.0, 0.0))
    val liveGpsCoords: LiveData<Pair<Double, Double>> = _liveGpsCoords

    private val _sessionPotholeCount = MutableLiveData(0)
    val sessionPotholeCount: LiveData<Int> = _sessionPotholeCount

    private val _lastDetectedPothole = MutableLiveData<PotholeEvent?>()
    val lastDetectedPothole: LiveData<PotholeEvent?> = _lastDetectedPothole

    fun setMonitoring(active: Boolean) {
        _isMonitoring.value = active
        if (!active) {
            _currentSpeedKmh.value = 0f
        }
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun onPotholeDetected(event: PotholeEvent) {
        _lastDetectedPothole.postValue(event)
        _sessionPotholeCount.postValue((_sessionPotholeCount.value ?: 0) + 1)
    }

    fun onLocationUpdated(lat: Double, lon: Double, speedKmh: Float) {
        _liveGpsCoords.postValue(Pair(lat, lon))
        _currentSpeedKmh.postValue(speedKmh)
    }

    data class VerificationState(
        val isVerifying: Boolean = false,
        val current: Int = 0,
        val total: Int = 0,
        val confirmed: Int = 0,
        val rejected: Int = 0,
        val isComplete: Boolean = false
    ) {
        val percentage: Int
            get() = if (total > 0) ((current.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
    }

    private val _verificationState = MutableLiveData(VerificationState())
    val verificationState: LiveData<VerificationState> = _verificationState

    fun updateVerificationProgress(current: Int, total: Int, confirmed: Int) {
        _verificationState.postValue(
            VerificationState(
                isVerifying = true,
                current = current,
                total = total,
                confirmed = confirmed,
                rejected = (current - confirmed).coerceAtLeast(0),
                isComplete = false
            )
        )
    }

    fun completeVerification(total: Int, confirmed: Int, rejected: Int) {
        _verificationState.postValue(
            VerificationState(
                isVerifying = false,
                current = total,
                total = total,
                confirmed = confirmed,
                rejected = rejected,
                isComplete = true
            )
        )
    }

    fun resetVerification() {
        _verificationState.value = VerificationState()
    }

    fun resetSessionCounter() {
        _sessionPotholeCount.value = 0
        _lastDetectedPothole.value = null
    }
}
