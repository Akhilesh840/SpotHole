package com.pothole.khadda.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pothole.khadda.KhaddaApplication
import com.pothole.khadda.R
import com.pothole.khadda.databinding.ActivityMainBinding
import com.pothole.khadda.databinding.DialogValidationProgressBinding
import com.pothole.khadda.detector.CandidateVerifier
import com.pothole.khadda.detector.PotholeVisionAnalyzer
import com.pothole.khadda.model.CandidateStatus
import com.pothole.khadda.service.DetectionForegroundService
import com.pothole.khadda.ui.authority.AuthorityFragment
import com.pothole.khadda.ui.history.HistoryFragment
import com.pothole.khadda.ui.map.MapFragment
import com.pothole.khadda.ui.monitor.MonitorFragment
import com.pothole.khadda.ui.monitor.MonitorViewModel
import com.pothole.khadda.ui.report.ReportFragment
import com.pothole.khadda.ui.settings.SettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val monitorViewModel: MonitorViewModel by viewModels()

    private val monitorFragment = MonitorFragment()
    private val mapFragment = MapFragment()
    private val historyFragment = HistoryFragment()
    private val authorityFragment = AuthorityFragment()
    private val reportFragment = ReportFragment()
    private val settingsFragment = SettingsFragment()

    private var detectionService: DetectionForegroundService? = null
    private var isServiceBound = false

    private var validationDialog: AlertDialog? = null
    private var dialogBinding: DialogValidationProgressBinding? = null

    private val progressListener = { current: Int, total: Int, confirmed: Int ->
        runOnUiThread {
            monitorViewModel.updateVerificationProgress(current, total, confirmed)
        }
    }

    private val completeListener = { total: Int, confirmed: Int, rejected: Int ->
        runOnUiThread {
            monitorViewModel.completeVerification(total, confirmed, rejected)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? DetectionForegroundService.LocalBinder
            val svc = binder?.getService() ?: return
            detectionService = svc
            isServiceBound = true

            svc.registerVerificationProgressListener(progressListener)
            svc.registerVerificationCompleteListener(completeListener)

            if (svc.isVerifying && svc.verificationTotal > 0) {
                monitorViewModel.updateVerificationProgress(
                    svc.verificationCurrent,
                    svc.verificationTotal,
                    svc.verificationConfirmed
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            detectionService?.unregisterVerificationProgressListener(progressListener)
            detectionService?.unregisterVerificationCompleteListener(completeListener)
            detectionService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(monitorFragment, "Khadda - Live Monitor")
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_monitor -> {
                    loadFragment(monitorFragment, "Khadda - Live Monitor")
                    true
                }
                R.id.nav_map -> {
                    loadFragment(mapFragment, "Road-Anomaly Map")
                    true
                }
                R.id.nav_history -> {
                    loadFragment(historyFragment, "Detection Log")
                    true
                }
                R.id.nav_authority -> {
                    loadFragment(authorityFragment, "Authority Dashboard")
                    true
                }
                R.id.nav_report -> {
                    loadFragment(reportFragment, "Analytics & Export")
                    true
                }
                else -> false
            }
        }

        binding.btnSettings.setOnClickListener {
            loadFragment(settingsFragment, "Algorithm Settings")
        }

        binding.toolbar.findViewById<View>(R.id.btnGallery).setOnClickListener {
            startActivity(Intent(this, DebugGalleryActivity::class.java))
        }

        binding.cardVerificationBanner.setOnClickListener {
            showVerificationPopup()
        }

        setupVerificationObservers()
        checkPendingCandidatesOnStartup()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, DetectionForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            detectionService?.unregisterVerificationProgressListener(progressListener)
            detectionService?.unregisterVerificationCompleteListener(completeListener)
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun setupVerificationObservers() {
        monitorViewModel.verificationState.observe(this) { state ->
            if (state.isVerifying) {
                // Persistent bottom banner visible across all tabs
                binding.cardVerificationBanner.visibility = View.VISIBLE
                binding.bannerProgressBar.max = if (state.total > 0) state.total else 100
                binding.bannerProgressBar.progress = state.current
                binding.tvBannerTitle.text = "Validating Potholes (${state.current}/${state.total})"
                binding.tvBannerPercent.text = "${state.percentage}%"
                binding.tvBannerSubtitle.text = "${state.confirmed} Confirmed • ${state.rejected} Rejected"

                // Update popup dialog if open
                dialogBinding?.let { db ->
                    db.progressBarDialog.max = if (state.total > 0) state.total else 100
                    db.progressBarDialog.progress = state.current
                    db.tvProgressCount.text = "${state.current} of ${state.total} scanned"
                    db.tvProgressPercentage.text = "${state.percentage}%"
                    db.tvConfirmedBadge.text = "✓ ${state.confirmed} Confirmed"
                    db.tvRejectedBadge.text = "✗ ${state.rejected} Rejected"
                }
            } else if (state.isComplete) {
                // Completed state
                binding.bannerProgressBar.progress = binding.bannerProgressBar.max
                binding.tvBannerTitle.text = "✓ Validation Complete"
                binding.tvBannerPercent.text = "100%"
                binding.tvBannerSubtitle.text = "${state.confirmed} confirmed • ${state.rejected} rejected"

                dialogBinding?.let { db ->
                    db.progressBarDialog.progress = db.progressBarDialog.max
                    db.tvDialogTitle.text = "Validation Complete"
                    db.tvDialogSubtitle.text = "Finished scanning all recorded road anomalies."
                    db.tvProgressCount.text = "${state.total} of ${state.total} scanned"
                    db.tvProgressPercentage.text = "100%"
                    db.tvConfirmedBadge.text = "✓ ${state.confirmed} Confirmed"
                    db.tvRejectedBadge.text = "✗ ${state.rejected} Rejected"
                    db.btnNavigateApp.text = "Done"
                }

                // Automatically hide banner after 5 seconds
                binding.cardVerificationBanner.postDelayed({
                    val current = monitorViewModel.verificationState.value
                    if (current == null || !current.isVerifying) {
                        binding.cardVerificationBanner.visibility = View.GONE
                    }
                }, 5000)
            } else {
                binding.cardVerificationBanner.visibility = View.GONE
            }
        }
    }

    /**
     * Shows a non-blocking dialog with real-time progress bar.
     * User can dismiss or click "Navigate App" to browse freely while scanning runs in the background.
     */
    fun showVerificationPopup(totalCount: Int? = null) {
        if (isFinishing || isDestroyed) return
        if (validationDialog?.isShowing == true) return

        val viewBinding = DialogValidationProgressBinding.inflate(layoutInflater)
        dialogBinding = viewBinding

        val state = monitorViewModel.verificationState.value ?: MonitorViewModel.VerificationState()
        val total = totalCount ?: if (state.total > 0) state.total else 1
        viewBinding.progressBarDialog.max = if (total > 0) total else 100
        viewBinding.progressBarDialog.progress = state.current
        viewBinding.tvProgressCount.text = "${state.current} of $total scanned"
        viewBinding.tvProgressPercentage.text = "${state.percentage}%"
        viewBinding.tvConfirmedBadge.text = "✓ ${state.confirmed} Confirmed"
        viewBinding.tvRejectedBadge.text = "✗ ${state.rejected} Rejected"

        val dialog = AlertDialog.Builder(this)
            .setView(viewBinding.root)
            .setCancelable(true) // Non-blocking: user can tap outside or press back
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        viewBinding.btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        viewBinding.btnNavigateApp.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Scanning in background. You can navigate the app freely.", Toast.LENGTH_SHORT).show()
        }

        viewBinding.btnCancelVerification.setOnClickListener {
            detectionService?.cancelVerification()
            monitorViewModel.resetVerification()
            dialog.dismiss()
            Toast.makeText(this, "Verification cancelled.", Toast.LENGTH_SHORT).show()
        }

        dialog.setOnDismissListener {
            validationDialog = null
            dialogBinding = null
        }

        dialog.show()
        validationDialog = dialog
    }

    /**
     * Resumability check: If app was killed mid-verification or ride ended with pending anomalies,
     * offers user to resume verification.
     */
    private fun checkPendingCandidatesOnStartup() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = (application as KhaddaApplication).repository
            val pending = repository.getPendingCandidates()
            if (pending.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    showPendingVerificationDialog(pending.size)
                }
            }
        }
    }

    private fun showPendingVerificationDialog(pendingCount: Int) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Unverified Road Anomalies")
            .setMessage("You have $pendingCount road anomalies from previous rides awaiting visual verification. Would you like to verify them now?")
            .setPositiveButton("Verify Now") { _, _ ->
                startBatchVerificationForPending()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun startBatchVerificationForPending() {
        showVerificationPopup()

        lifecycleScope.launch(Dispatchers.Default) {
            val repository = (application as KhaddaApplication).repository
            val analyzer = PotholeVisionAnalyzer(applicationContext)
            val pending = repository.getPendingCandidates()
            var confirmed = 0
            var rejected = 0

            withContext(Dispatchers.Main) {
                monitorViewModel.updateVerificationProgress(0, pending.size, 0)
            }

            for ((idx, candidate) in pending.withIndex()) {
                val status = CandidateVerifier.verifyCandidate(
                    context = applicationContext,
                    candidate = candidate,
                    analyzer = analyzer,
                    repository = repository
                )
                if (status == CandidateStatus.CONFIRMED) confirmed++ else rejected++

                withContext(Dispatchers.Main) {
                    monitorViewModel.updateVerificationProgress(idx + 1, pending.size, confirmed)
                }
            }
            analyzer.close()

            withContext(Dispatchers.Main) {
                monitorViewModel.completeVerification(pending.size, confirmed, rejected)
                Toast.makeText(
                    this@MainActivity,
                    "Verification complete: $confirmed confirmed, $rejected rejected.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        binding.toolbar.title = title
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                monitorFragment.triggerManualPotholeFromVolumeKey("MANUAL_POTHOLE")
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                monitorFragment.triggerManualPotholeFromVolumeKey("MANUAL_SPEED_BREAKER")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
