package com.pothole.khadda.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.pothole.khadda.R
import com.pothole.khadda.databinding.FragmentMapBinding
import com.pothole.khadda.model.CandidateEvent
import com.pothole.khadda.model.PotholeEvent
import com.pothole.khadda.model.SeverityLevel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by activityViewModels()
    private var allEvents: List<PotholeEvent> = emptyList()
    private var pendingCandidates: List<CandidateEvent> = emptyList()
    private var selectedSeverityFilter: SeverityLevel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup OpenStreetMap
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(14.0)

        // Default center (Pune / India or first event)
        val defaultPoint = GeoPoint(18.5204, 73.8567)
        mapController.setCenter(defaultPoint)

        binding.btnCloseMarkerDetail.setOnClickListener {
            binding.cardMarkerDetail.visibility = View.GONE
        }

        binding.fabMyLocation.setOnClickListener {
            val target = allEvents.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }?.let {
                GeoPoint(it.latitude, it.longitude)
            } ?: pendingCandidates.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }?.let {
                GeoPoint(it.latitude, it.longitude)
            } ?: defaultPoint

            mapController.animateTo(target)
            mapController.setZoom(16.0)
        }

        // Chip Filters
        binding.chipGroupSeverity.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedSeverityFilter = when {
                checkedIds.contains(R.id.chipHigh) -> SeverityLevel.HIGH
                checkedIds.contains(R.id.chipMedium) -> SeverityLevel.MEDIUM
                checkedIds.contains(R.id.chipLow) -> SeverityLevel.LOW
                else -> null
            }
            refreshMarkers()
        }

        viewModel.allEvents.observe(viewLifecycleOwner) { events ->
            allEvents = events
            refreshMarkers()
            if (events.isNotEmpty()) {
                val firstWithLocation = events.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }
                if (firstWithLocation != null) {
                    mapController.setCenter(GeoPoint(firstWithLocation.latitude, firstWithLocation.longitude))
                }
            }
        }

        viewModel.pendingCandidates.observe(viewLifecycleOwner) { candidates ->
            pendingCandidates = candidates
            refreshMarkers()
        }
    }

    private fun refreshMarkers() {
        binding.mapView.overlays.clear()

        val filteredEvents = if (selectedSeverityFilter == null) {
            allEvents
        } else {
            allEvents.filter { it.severity == selectedSeverityFilter }
        }

        // 1. Confirmed Pothole Events (recolored by severity: Green, Yellow, Red)
        for (event in filteredEvents) {
            if (event.latitude == 0.0 && event.longitude == 0.0) continue

            val point = GeoPoint(event.latitude, event.longitude)
            val marker = Marker(binding.mapView)
            marker.position = point
            marker.title = "${event.severity} Pothole - ${event.algorithm}"
            marker.snippet = String.format(Locale.US, "Impact: %.2f m/s² | Status: %s", event.zDiffValue, event.status)

            val markerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pothole_marker)?.mutate()
            val color = when (event.severity) {
                SeverityLevel.HIGH -> Color.parseColor("#EF4444")
                SeverityLevel.MEDIUM -> Color.parseColor("#F59E0B")
                SeverityLevel.LOW -> Color.parseColor("#10B981")
            }
            markerDrawable?.setTint(color)
            marker.icon = markerDrawable

            marker.setOnMarkerClickListener { m, _ ->
                showMarkerDetail(event)
                binding.mapView.controller.animateTo(m.position)
                true
            }

            binding.mapView.overlays.add(marker)
        }

        // 2. Pending Candidates during ride (Gray markers)
        for (candidate in pendingCandidates) {
            if (candidate.latitude == 0.0 && candidate.longitude == 0.0) continue
            if (selectedSeverityFilter != null && candidate.severity != selectedSeverityFilter) continue

            val point = GeoPoint(candidate.latitude, candidate.longitude)
            val marker = Marker(binding.mapView)
            marker.position = point
            marker.title = "Pending Anomaly (${candidate.severity})"
            marker.snippet = String.format(Locale.US, "Impact: %.2f m/s² | Status: PENDING VERIFICATION", candidate.impact)

            val markerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pothole_marker)?.mutate()
            markerDrawable?.setTint(Color.parseColor("#9CA3AF")) // Gray marker for pending
            marker.icon = markerDrawable

            marker.setOnMarkerClickListener { m, _ ->
                showCandidateMarkerDetail(candidate)
                binding.mapView.controller.animateTo(m.position)
                true
            }

            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun showMarkerDetail(event: PotholeEvent) {
        binding.cardMarkerDetail.visibility = View.VISIBLE
        binding.tvMarkerSeverity.text = event.severity.name
        binding.tvMarkerAlgorithm.text = String.format(Locale.US, "%s (Impact: %.2f m/s²)", event.algorithm, event.zDiffValue)
        binding.tvMarkerLocation.text = String.format(Locale.US, "Location: %.5f°, %.5f°", event.latitude, event.longitude)
        binding.tvMarkerStatus.text = "Status: ${event.status.name.replace("_", " ")}"

        val color = when (event.severity) {
            SeverityLevel.HIGH -> Color.parseColor("#EF4444")
            SeverityLevel.MEDIUM -> Color.parseColor("#F59E0B")
            SeverityLevel.LOW -> Color.parseColor("#10B981")
        }
        binding.tvMarkerSeverity.setBackgroundColor(color)
    }

    private fun showCandidateMarkerDetail(candidate: CandidateEvent) {
        binding.cardMarkerDetail.visibility = View.VISIBLE
        binding.tvMarkerSeverity.text = "PENDING (${candidate.severity.name})"
        binding.tvMarkerAlgorithm.text = String.format(Locale.US, "Impact: %.2f m/s² (Awaiting Verification)", candidate.impact)
        binding.tvMarkerLocation.text = String.format(Locale.US, "Location: %.5f°, %.5f°", candidate.latitude, candidate.longitude)
        binding.tvMarkerStatus.text = "Status: PENDING VERIFICATION"
        binding.tvMarkerSeverity.setBackgroundColor(Color.parseColor("#6B7280"))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
