package com.pothole.khadda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.pothole.khadda.BuildConfig
import com.pothole.khadda.KhaddaApplication
import com.pothole.khadda.detector.*
import com.pothole.khadda.location.LocationTracker
import com.pothole.khadda.model.*
import com.pothole.khadda.sensor.SensorCollector
import com.pothole.khadda.sensor.SensorStreamTracker
import com.pothole.khadda.simulation.RoadSimulationManager
import com.pothole.khadda.ui.MainActivity
import com.pothole.khadda.utils.AlertHelper
import com.pothole.khadda.utils.ModelMetadataUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs

/**
 * Foreground Service that keeps accelerometer and GPS sensing active continuously
 * even when the phone screen is locked or the user navigates to other apps.
 *
 * Architecture (Post-Ride Batch Verification):
 * - During ride: Physical sensors (2-vote rule + kinematic filters) log CandidateEvents.
 * - Copied frames are dispatched via a Channel to a single background IO worker coroutine,
 *   which compresses JPEGs (quality 90) and stores CandidateEvents with status PENDING.
 * - Zero model inference runs during the ride, keeping the 100 Hz sensor stream completely intact.
 */
class DetectionForegroundService : LifecycleService() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private lateinit var sensorCollector: SensorCollector
    private lateinit var locationTracker: LocationTracker
    private lateinit var dataProcessor: DataProcessor
    private lateinit var detector: PotholeDetector
    private lateinit var falsePositiveFilter: FalsePositiveFilter
    private lateinit var visionAnalyzer: PotholeVisionAnalyzer
    private lateinit var alertHelper: AlertHelper
    private lateinit var simulationManager: RoadSimulationManager

    var currentSession: DetectionSession? = null
        private set

    var isDetectionActive: Boolean = false
        private set

    var isSimulationMode: Boolean = false

    // Callbacks for UI subscribers
    var onLiveSensorData: ((SensorData) -> Unit)? = null
    var onPotholeDetected: ((PotholeEvent) -> Unit)? = null
    var onCandidateLogged: ((CandidateEvent) -> Unit)? = null
    var onLocationUpdated: ((Double, Double, Float) -> Unit)? = null
    var onVerificationProgress: ((current: Int, total: Int, confirmedCount: Int) -> Unit)? = null
    var onVerificationComplete: ((total: Int, confirmedCount: Int, rejectedCount: Int) -> Unit)? = null

    var isVerifying: Boolean = false
        private set
    var verificationTotal: Int = 0
        private set
    var verificationCurrent: Int = 0
        private set
    var verificationConfirmed: Int = 0
        private set
    var verificationRejected: Int = 0
        private set

    private val verificationProgressListeners = mutableListOf<(current: Int, total: Int, confirmedCount: Int) -> Unit>()
    private val verificationCompleteListeners = mutableListOf<(total: Int, confirmedCount: Int, rejectedCount: Int) -> Unit>()

    fun registerVerificationProgressListener(listener: (current: Int, total: Int, confirmedCount: Int) -> Unit) {
        synchronized(verificationProgressListeners) {
            if (!verificationProgressListeners.contains(listener)) {
                verificationProgressListeners.add(listener)
            }
        }
        if (isVerifying && verificationTotal > 0) {
            listener.invoke(verificationCurrent, verificationTotal, verificationConfirmed)
        }
    }

    fun unregisterVerificationProgressListener(listener: (current: Int, total: Int, confirmedCount: Int) -> Unit) {
        synchronized(verificationProgressListeners) {
            verificationProgressListeners.remove(listener)
        }
    }

    fun registerVerificationCompleteListener(listener: (total: Int, confirmedCount: Int, rejectedCount: Int) -> Unit) {
        synchronized(verificationCompleteListeners) {
            if (!verificationCompleteListeners.contains(listener)) {
                verificationCompleteListeners.add(listener)
            }
        }
    }

    fun unregisterVerificationCompleteListener(listener: (total: Int, confirmedCount: Int, rejectedCount: Int) -> Unit) {
        synchronized(verificationCompleteListeners) {
            verificationCompleteListeners.remove(listener)
        }
    }

    private var verificationJob: Job? = null

    private lateinit var telemetryLogger: TelemetryLogger
    private val streamTracker = SensorStreamTracker()

    private var candidateCount = 0
    private var potholeCount = 0

    private var lastSpeed = 0f
    private var lastLocationTime = 0L
    private var currentGpsAccel = 0f

    private val frameBuffer = ArrayDeque<Pair<Long, Bitmap>>()
    private var lastFrameTime = 0L
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null

    var isTorchEnabled = false

    // Background Channel for offloading disk I/O and JPEG compression
    private val candidateChannel = Channel<CandidateWriteRequest>(capacity = 64)

    private data class CandidateWriteRequest(
        val candidateId: String,
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
        val frames: List<Bitmap>
    )

    private data class PendingVibrationCapture(
        val candidateId: String,
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
        val targetFrames: Int = MAX_CANDIDATE_FRAMES,
        val collectedFrames: MutableList<Bitmap> = mutableListOf(),
        val createdAtMs: Long = System.currentTimeMillis()
    )

    private val pendingVibrationCaptures = java.util.Collections.synchronizedList(mutableListOf<PendingVibrationCapture>())

    private fun dispatchCandidateRequest(cap: PendingVibrationCapture) {
        val request = CandidateWriteRequest(
            candidateId = cap.candidateId,
            sessionId = cap.sessionId,
            timestampMs = cap.timestampMs,
            latitude = cap.latitude,
            longitude = cap.longitude,
            speedMps = cap.speedMps,
            triggeredZDiff = cap.triggeredZDiff,
            triggeredZThresh = cap.triggeredZThresh,
            triggeredStdev = cap.triggeredStdev,
            triggeredGZero = cap.triggeredGZero,
            zDiff = cap.zDiff,
            zThresh = cap.zThresh,
            stdev = cap.stdev,
            impact = cap.impact,
            severity = cap.severity,
            frames = cap.collectedFrames
        )
        val sendResult = candidateChannel.trySend(request)
        if (!sendResult.isSuccess) {
            serviceScope.launch(Dispatchers.IO) {
                candidateChannel.send(request)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): DetectionForegroundService = this@DetectionForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        sensorCollector = SensorCollector(this)
        locationTracker = LocationTracker(this)
        dataProcessor = DataProcessor()
        detector = PotholeDetector()
        falsePositiveFilter = FalsePositiveFilter()
        alertHelper = AlertHelper(this)
        simulationManager = RoadSimulationManager()
        telemetryLogger = TelemetryLogger(this)
        visionAnalyzer = PotholeVisionAnalyzer(this)

        setupListeners()
        startCandidateWriterWorker()
    }

    private fun startCandidateWriterWorker() {
        serviceScope.launch(Dispatchers.IO) {
            for (request in candidateChannel) {
                processCandidateWriteRequest(request)
            }
        }
    }

    private suspend fun processCandidateWriteRequest(request: CandidateWriteRequest) {
        val sessionDir = File(applicationContext.filesDir, "sessions/${request.sessionId}/candidates")
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        val savedPaths = mutableListOf<String>()
        for ((idx, bitmap) in request.frames.withIndex()) {
            val file = File(sessionDir, "cand_${request.candidateId}_$idx.jpg")
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }
                savedPaths.add(file.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error compressing candidate JPEG: ${e.message}", e)
            } finally {
                bitmap.recycle() // Release copied bitmap memory immediately
            }
        }

        val candidate = CandidateEvent(
            id = request.candidateId,
            sessionId = request.sessionId,
            timestampMs = request.timestampMs,
            latitude = request.latitude,
            longitude = request.longitude,
            speedMps = request.speedMps,
            triggeredZDiff = request.triggeredZDiff,
            triggeredZThresh = request.triggeredZThresh,
            triggeredStdev = request.triggeredStdev,
            triggeredGZero = request.triggeredGZero,
            zDiff = request.zDiff,
            zThresh = request.zThresh,
            stdev = request.stdev,
            impact = request.impact,
            severity = request.severity,
            status = CandidateStatus.PENDING,
            maxConfidence = 0.0f,
            framePaths = savedPaths,
            inferenceMsPerFrame = 0L,
            modelFileName = PotholeVisionAnalyzer.DEFAULT_MODEL_FILE,
            createdAt = System.currentTimeMillis()
        )

        try {
            val repo = (application as KhaddaApplication).repository
            repo.insertCandidate(candidate)
            onCandidateLogged?.invoke(candidate)
            Log.d(TAG, "Successfully persisted candidate ${candidate.id} (frames: ${savedPaths.size}) with status PENDING")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist candidate event: ${e.message}", e)
        }
    }

    private fun setupListeners() {
        // Hardware sensor listener
        sensorCollector.onSensorDataListener = { data ->
            if (isDetectionActive && !isSimulationMode) {
                processSensorSample(data)
            }
        }

        // Hardware GPS listener
        locationTracker.onLocationChangedListener = { loc ->
            val now = System.currentTimeMillis()
            if (lastLocationTime != 0L) {
                val dt = (now - lastLocationTime) / 1000f
                if (dt > 0f) {
                    currentGpsAccel = (loc.speed - lastSpeed) / dt
                }
            }
            lastSpeed = loc.speed
            lastLocationTime = now

            val elapsedNanos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                loc.elapsedRealtimeNanos
            } else {
                android.os.SystemClock.elapsedRealtimeNanos()
            }
            telemetryLogger.logGps(
                elapsedNanos, loc.latitude, loc.longitude, loc.speed, loc.accuracy, loc.bearing
            )

            onLocationUpdated?.invoke(loc.latitude, loc.longitude, loc.speed)
        }

        // Simulation listeners
        simulationManager.onSimulatedSensorData = { data ->
            if (isDetectionActive && isSimulationMode) {
                processSensorSample(data)
            }
        }

        simulationManager.onSimulatedLocation = { lat, lon, speedKmh ->
            lastSpeed = speedKmh / 3.6f
            lastLocationTime = System.currentTimeMillis()
            onLocationUpdated?.invoke(lat, lon, lastSpeed)
        }
    }

    /**
     * Sensor processing callback executed at 100 Hz.
     * Contains only lightweight math and in-memory bitmap copying.
     * ZERO disk I/O and ZERO model inference.
     */
    private fun processSensorSample(data: SensorData) {
        val startNs = System.nanoTime()
        streamTracker.recordSampleArrival(data.timestampNs)
        try {
            val timeSinceStart = System.currentTimeMillis() - (currentSession?.startTime ?: 0L)
            if (timeSinceStart < 1000L) return // Drop first second to let gravity stabilize

        onLiveSensorData?.invoke(data)

        // 1. Extract signal features and update rolling window
        val features = dataProcessor.extractFeatures(data, currentGpsAccel)

        // Log continuous telemetry
        val speedAgeMs = System.currentTimeMillis() - lastLocationTime
        telemetryLogger.logSensor(
            data.timestampNs, features.vertAccel, features.meanZ, features.accelMagnitude,
            features.zDiff, features.stdevZ, features.zThreshDiff,
            features.yaw, features.roll, features.pitch, lastSpeed, speedAgeMs,
            data.accelX.toFloat(), data.accelY.toFloat(), data.accelZ.toFloat(),
            data.gravX.toFloat(), data.gravY.toFloat(), data.gravZ.toFloat(),
            data.linX.toFloat(), data.linY.toFloat(), data.linZ.toFloat(),
            data.gyroX.toFloat(), data.gyroY.toFloat(), data.gyroZ.toFloat()
        )

        // Obtain location for candidate logging
        val (lat, lon) = if (isSimulationMode) {
            val loc = locationTracker.currentLocation
            Pair(loc?.latitude ?: 18.5204, loc?.longitude ?: 73.8567)
        } else {
            val loc = locationTracker.currentLocation
            Pair(loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
        }

        // 2. Evaluate physical detection algorithms
        val triggered = detector.getTriggeredAlgorithms(features)
        val isCandidate = triggered.isNotEmpty() || features.zDiff >= (detector.zDiffThreshold / 2.0)

        if (!isCandidate) return

        var rejectedReason = ""
        var isValid = true

        // Require 2-vote rule
        if (triggered.size < 2) {
            isValid = false
            rejectedReason = "Failed 2-vote rule (Got ${triggered.size})"
        } else {
            // Kinematic false-positive validation (braking, turning, cooldown)
            val validation = falsePositiveFilter.validateTrigger(features, lat, lon, lastSpeed)
            if (!validation.isValid) {
                isValid = false
                rejectedReason = validation.reason
            }
        }

        // Cap candidates per session at MAX_CANDIDATES_PER_SESSION
        if (isValid && candidateCount >= MAX_CANDIDATES_PER_SESSION) {
            Log.w(TAG, "Candidate cap reached ($MAX_CANDIDATES_PER_SESSION). Dropping candidate.")
            isValid = false
            rejectedReason = "Filtered: Session candidate cap ($MAX_CANDIDATES_PER_SESSION) reached"
        }

        telemetryLogger.logCandidate(
            data.timestampNs, lat, lon, lastSpeed, features.yaw,
            features.zDiff, features.stdevZ, features.zThreshDiff,
            triggered.contains(AlgorithmType.Z_DIFF),
            triggered.contains(AlgorithmType.Z_THRESH),
            triggered.contains(AlgorithmType.STDEV_Z),
            triggered.contains(AlgorithmType.G_ZERO),
            triggered.size, rejectedReason
        )

        if (!isValid) return

        // 3. Candidate Accepted by Physical Sensors!
        candidateCount++
        val result = detector.buildResult(features, triggered)

        // Instant feedback: alert sound and notification update
        alertHelper.triggerAlert(result.severity)
        updateNotification("Road Anomalies Logged: $candidateCount (Pending Verification)")

        // 4. Capture 5 frames after vibration detected
        val candidateId = UUID.randomUUID().toString()
        val capture = PendingVibrationCapture(
            candidateId = candidateId,
            sessionId = currentSession?.sessionId ?: "",
            timestampMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lon,
            speedMps = lastSpeed,
            triggeredZDiff = triggered.contains(AlgorithmType.Z_DIFF),
            triggeredZThresh = triggered.contains(AlgorithmType.Z_THRESH),
            triggeredStdev = triggered.contains(AlgorithmType.STDEV_Z),
            triggeredGZero = triggered.contains(AlgorithmType.G_ZERO),
            zDiff = features.zDiff,
            zThresh = features.zThreshDiff,
            stdev = features.stdevZ,
            impact = result.impactMagnitude,
            severity = result.severity,
            targetFrames = MAX_CANDIDATE_FRAMES
        )

        if (cameraProvider == null || isSimulationMode) {
            val copiedFrames = mutableListOf<Bitmap>()
            synchronized(frameBuffer) {
                for (item in frameBuffer.toList().takeLast(MAX_CANDIDATE_FRAMES)) {
                    if (!item.second.isRecycled) {
                        try {
                            item.second.copy(Bitmap.Config.ARGB_8888, false)?.let { copiedFrames.add(it) }
                        } catch (e: Exception) {}
                    }
                }
            }
            capture.collectedFrames.addAll(copiedFrames)
            dispatchCandidateRequest(capture)
        } else {
            // Camera active: collect the next 5 incoming frames after vibration
            synchronized(pendingVibrationCaptures) {
                pendingVibrationCaptures.add(capture)
            }
        }
    } finally {
        streamTracker.recordCallbackDuration(System.nanoTime() - startNs)
    }
}

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val now = System.currentTimeMillis()
                if (now - lastFrameTime >= 200) {
                    lastFrameTime = now
                    val bitmap = imageProxy.toBitmap()
                    val timestampNs = imageProxy.imageInfo.timestamp

                    synchronized(frameBuffer) {
                        frameBuffer.addLast(Pair(timestampNs, bitmap))
                        // Keep ring buffer bounded at 25 frames (~5s at 5 FPS)
                        if (frameBuffer.size > 25) {
                            val evicted = frameBuffer.removeFirst()
                            if (!evicted.second.isRecycled) {
                                evicted.second.recycle() // Recycle evicted frame to reduce GC stutter
                            }
                        }
                    }

                    // Deliver incoming post-vibration frames to active captures
                    val completedCaptures = mutableListOf<PendingVibrationCapture>()
                    synchronized(pendingVibrationCaptures) {
                        val iterator = pendingVibrationCaptures.iterator()
                        while (iterator.hasNext()) {
                            val capture = iterator.next()
                            if (capture.collectedFrames.size < capture.targetFrames) {
                                try {
                                    val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    if (copy != null) {
                                        capture.collectedFrames.add(copy)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to copy post-vibration frame: ${e.message}")
                                }
                            }
                            if (capture.collectedFrames.size >= capture.targetFrames || (now - capture.createdAtMs > 4000L)) {
                                completedCaptures.add(capture)
                                iterator.remove()
                            }
                        }
                    }

                    for (cap in completedCaptures) {
                        dispatchCandidateRequest(cap)
                    }
                }
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                val camera = cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis)
                cameraControl = camera?.cameraControl
                cameraControl?.enableTorch(isTorchEnabled)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun toggleTorch(enable: Boolean) {
        isTorchEnabled = enable
        cameraControl?.enableTorch(enable)
    }

    fun triggerManualPothole(eventType: String = "MANUAL_TRIGGER", forceAiResult: Boolean? = null) {
        telemetryLogger.markNextRowWithLabel(eventType)

        val lat = locationTracker.currentLocation?.latitude ?: 0.0
        val lon = locationTracker.currentLocation?.longitude ?: 0.0
        val session = currentSession

        if (candidateCount >= MAX_CANDIDATES_PER_SESSION) {
            Log.w(TAG, "Candidate cap reached ($MAX_CANDIDATES_PER_SESSION)")
            return
        }

        candidateCount++
        alertHelper.triggerAlert(SeverityLevel.HIGH)
        updateNotification("Road Anomalies Logged: $candidateCount (Manual Trigger)")

        val candidateId = UUID.randomUUID().toString()
        val capture = PendingVibrationCapture(
            candidateId = candidateId,
            sessionId = session?.sessionId ?: "",
            timestampMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lon,
            speedMps = lastSpeed,
            triggeredZDiff = true,
            triggeredZThresh = true,
            triggeredStdev = true,
            triggeredGZero = false,
            zDiff = 9.99,
            zThresh = 9.99,
            stdev = 5.0,
            impact = 9.99,
            severity = SeverityLevel.HIGH,
            targetFrames = MAX_CANDIDATE_FRAMES
        )

        if (cameraProvider == null || isSimulationMode) {
            val copiedFrames = mutableListOf<Bitmap>()
            synchronized(frameBuffer) {
                for (item in frameBuffer.toList().takeLast(MAX_CANDIDATE_FRAMES)) {
                    if (!item.second.isRecycled) {
                        try {
                            item.second.copy(Bitmap.Config.ARGB_8888, false)?.let { copiedFrames.add(it) }
                        } catch (e: Exception) {}
                    }
                }
            }
            capture.collectedFrames.addAll(copiedFrames)
            dispatchCandidateRequest(capture)
        } else {
            synchronized(pendingVibrationCaptures) {
                pendingVibrationCaptures.add(capture)
            }
        }
    }

    fun startDetection(useSimulation: Boolean = false): Boolean {
        if (isDetectionActive) return true

        isSimulationMode = useSimulation
        candidateCount = 0
        potholeCount = 0
        dataProcessor.reset()
        falsePositiveFilter.reset()
        streamTracker.reset()

        val newSession = DetectionSession(
            sessionId = UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis(),
            status = SessionStatus.RUNNING
        )
        currentSession = newSession

        serviceScope.launch {
            (application as KhaddaApplication).repository.insertSession(newSession)
        }

        startForeground(NOTIFICATION_ID, buildNotification("Pothole Detection Active - Monitoring Road..."))

        val modelSha = ModelMetadataUtil.getModelSha256(this)
        val started = if (isSimulationMode) {
            simulationManager.startSimulation(serviceScope)
            val meta = """
                Simulation: true
                GitCommit: ${BuildConfig.GIT_COMMIT_HASH}
                ModelFileName: ${PotholeVisionAnalyzer.DEFAULT_MODEL_FILE}
                ModelSha256: $modelSha
                ConfidenceThreshold: ${VisionResult.CONFIDENCE_THRESHOLD}
                MaxCandidateFrames: $MAX_CANDIDATE_FRAMES
                MaxCandidatesPerSession: $MAX_CANDIDATES_PER_SESSION
                Thresholds: ZDiff=${detector.zDiffThreshold}, Stdev=${detector.stdevThreshold}, ZThresh=${detector.zThreshThreshold}, GZero=${detector.gZeroThreshold}
            """.trimIndent()
            telemetryLogger.startLogging(newSession.sessionId, meta)
            true
        } else {
            locationTracker.startLocationUpdates()
            startCamera()
            val success = sensorCollector.startSensors()
            if (success) {
                val meta = """
                    Simulation: false
                    GitCommit: ${BuildConfig.GIT_COMMIT_HASH}
                    ModelFileName: ${PotholeVisionAnalyzer.DEFAULT_MODEL_FILE}
                    ModelSha256: $modelSha
                    ConfidenceThreshold: ${VisionResult.CONFIDENCE_THRESHOLD}
                    MaxCandidateFrames: $MAX_CANDIDATE_FRAMES
                    MaxCandidatesPerSession: $MAX_CANDIDATES_PER_SESSION
                    Vehicle: Bike
                    Mount: Handlebar
                    Phone: ${Build.MODEL}
                    Android: ${Build.VERSION.RELEASE}
                    StartTime: ${java.util.Date()}
                    Thresholds: ZDiff=${detector.zDiffThreshold}, Stdev=${detector.stdevThreshold}, ZThresh=${detector.zThreshThreshold}, GZero=${detector.gZeroThreshold}
                """.trimIndent()
                telemetryLogger.startLogging(newSession.sessionId, meta)
            }
            success
        }

        isDetectionActive = started
        return started
    }

    fun stopDetection(): DetectionSession? {
        stopMonitoringAndVerify()
        return currentSession
    }

    /**
     * Stops sensor acquisition immediately to free CPU/Looper,
     * keeps foreground service alive, and runs post-ride batch verification on Dispatchers.Default.
     */
    fun stopMonitoringAndVerify(onFinish: ((DetectionSession?) -> Unit)? = null) {
        if (!isDetectionActive && !isVerifying) {
            onFinish?.invoke(currentSession)
            return
        }

        val session = currentSession
        val sessionId = session?.sessionId ?: ""

        // 1. Immediately shut down hardware sensor streaming
        isDetectionActive = false

        // Flush any active post-vibration captures before closing
        synchronized(pendingVibrationCaptures) {
            for (cap in pendingVibrationCaptures) {
                dispatchCandidateRequest(cap)
            }
            pendingVibrationCaptures.clear()
        }
        val streamMetrics = streamTracker.getMetrics()
        val metricsStr = streamMetrics.toTelemetryString()
        Log.i(TAG, "Sensor stream metrics:\n$metricsStr")
        if (sessionId.isNotEmpty()) {
            telemetryLogger.appendMetadata(sessionId, metricsStr)
        }
        telemetryLogger.stopLogging()
        sensorCollector.stopSensors()
        locationTracker.stopLocationUpdates()
        simulationManager.stopSimulation()
        cameraProvider?.unbindAll()

        synchronized(frameBuffer) {
            for (frame in frameBuffer) {
                if (!frame.second.isRecycled) {
                    frame.second.recycle()
                }
            }
            frameBuffer.clear()
        }

        // 2. Start batch verification inside serviceScope on Dispatchers.Default
        isVerifying = true

        verificationJob = serviceScope.launch(Dispatchers.Default) {
            // Short grace period to ensure channel disk writer drains
            delay(350)

            val repository = (application as KhaddaApplication).repository
            val pendingCandidates = if (sessionId.isNotEmpty()) {
                repository.getPendingCandidatesForSession(sessionId)
            } else {
                repository.getPendingCandidates()
            }

            if (pendingCandidates.isEmpty()) {
                Log.i(TAG, "No pending candidates to verify for session '$sessionId'")
                completeVerification(session, 0, 0, 0, onFinish)
                return@launch
            }

            Log.i(TAG, "Starting post-ride verification of ${pendingCandidates.size} candidates on Dispatchers.Default")
            var confirmedCount = 0
            var rejectedCount = 0
            var errorCount = 0

            verificationTotal = pendingCandidates.size
            verificationCurrent = 0
            verificationConfirmed = 0
            verificationRejected = 0

            updateVerificationNotification(0, pendingCandidates.size, 0)
            withContext(Dispatchers.Main) {
                onVerificationProgress?.invoke(0, pendingCandidates.size, 0)
                synchronized(verificationProgressListeners) {
                    for (l in verificationProgressListeners) {
                        l.invoke(0, pendingCandidates.size, 0)
                    }
                }
            }

            for ((index, candidate) in pendingCandidates.withIndex()) {
                if (!isActive) {
                    Log.i(TAG, "Post-ride verification cancelled by user (Later option)")
                    break
                }

                val status = CandidateVerifier.verifyCandidate(
                    context = applicationContext,
                    candidate = candidate,
                    analyzer = visionAnalyzer,
                    repository = repository
                )

                when (status) {
                    CandidateStatus.CONFIRMED -> confirmedCount++
                    CandidateStatus.REJECTED -> rejectedCount++
                    CandidateStatus.ERROR -> errorCount++
                    CandidateStatus.PENDING -> {}
                }

                val currentStep = index + 1
                verificationCurrent = currentStep
                verificationConfirmed = confirmedCount
                verificationRejected = rejectedCount

                updateVerificationNotification(currentStep, pendingCandidates.size, confirmedCount)
                withContext(Dispatchers.Main) {
                    onVerificationProgress?.invoke(currentStep, pendingCandidates.size, confirmedCount)
                    synchronized(verificationProgressListeners) {
                        for (l in verificationProgressListeners) {
                            l.invoke(currentStep, pendingCandidates.size, confirmedCount)
                        }
                    }
                }
            }

            completeVerification(session, pendingCandidates.size, confirmedCount, rejectedCount, onFinish)
        }
    }

    private suspend fun completeVerification(
        session: DetectionSession?,
        total: Int,
        confirmedCount: Int,
        rejectedCount: Int,
        onFinish: ((DetectionSession?) -> Unit)?
    ) {
        isVerifying = false
        potholeCount = confirmedCount

        if (session != null) {
            session.endTime = System.currentTimeMillis()
            session.status = SessionStatus.STOPPED
            session.totalEvents = confirmedCount

            val repo = (application as KhaddaApplication).repository
            repo.closeSession(session.sessionId, confirmedCount)
        }

        withContext(Dispatchers.Main) {
            onVerificationComplete?.invoke(total, confirmedCount, rejectedCount)
            synchronized(verificationCompleteListeners) {
                for (l in verificationCompleteListeners) {
                    l.invoke(total, confirmedCount, rejectedCount)
                }
            }
            onFinish?.invoke(session)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * User chose "Later". Gracefully cancels the ongoing verification job and stops service.
     */
    fun cancelVerification() {
        verificationJob?.cancel()
        isVerifying = false
        verificationCurrent = 0
        verificationTotal = 0
        val session = currentSession
        if (session != null) {
            session.endTime = System.currentTimeMillis()
            session.status = SessionStatus.STOPPED
            serviceScope.launch {
                (application as KhaddaApplication).repository.updateSession(session)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateVerificationNotification(current: Int, total: Int, confirmedCount: Int) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Khadda - Validating Road Anomalies")
            .setContentText("Validating $current of $total anomalies ($confirmedCount confirmed)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setProgress(total, current, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun getDetector(): PotholeDetector = detector
    fun getAlertHelper(): AlertHelper = alertHelper
    fun getCandidateCount(): Int = candidateCount

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pothole Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification for real-time pothole sensing"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Khadda Pothole Detection")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
        alertHelper.release()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "DetectionService"
        const val CHANNEL_ID = "khadda_detection_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val MAX_CANDIDATE_FRAMES = 5
        const val MAX_CANDIDATES_PER_SESSION = 300
    }
}
