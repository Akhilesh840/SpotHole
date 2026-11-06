package com.pothole.khadda.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pothole.khadda.KhaddaApplication
import com.pothole.khadda.R
import com.pothole.khadda.model.CandidateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugGalleryActivity : AppCompatActivity() {

    private lateinit var imageContainer: LinearLayout
    private var isShowingRejected = true

    data class GalleryItem(
        val file: File,
        val title: String,
        val subtitle: String,
        val isConfirmed: Boolean,
        val candidateId: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_gallery)

        imageContainer = findViewById(R.id.imageContainer)

        val btnConfirmed = findViewById<Button>(R.id.btnShowConfirmed)
        val btnRejected = findViewById<Button>(R.id.btnShowRejected)

        btnConfirmed.setOnClickListener {
            isShowingRejected = false
            updateTabButtons(btnConfirmed, btnRejected)
            loadGallery(false)
        }

        btnRejected.setOnClickListener {
            isShowingRejected = true
            updateTabButtons(btnConfirmed, btnRejected)
            loadGallery(true)
        }

        // Default to showing rejected images to facilitate validation debugging
        updateTabButtons(btnConfirmed, btnRejected)
        loadGallery(true)
    }

    private fun updateTabButtons(btnConfirmed: Button, btnRejected: Button) {
        if (isShowingRejected) {
            btnRejected.setBackgroundColor(Color.parseColor("#D32F2F")) // Red highlight
            btnRejected.setTextColor(Color.WHITE)
            btnConfirmed.setBackgroundColor(Color.parseColor("#333333"))
            btnConfirmed.setTextColor(Color.LTGRAY)
        } else {
            btnConfirmed.setBackgroundColor(Color.parseColor("#388E3C")) // Green highlight
            btnConfirmed.setTextColor(Color.WHITE)
            btnRejected.setBackgroundColor(Color.parseColor("#333333"))
            btnRejected.setTextColor(Color.LTGRAY)
        }
    }

    private fun loadGallery(showRejected: Boolean) {
        imageContainer.removeAllViews()

        val progress = TextView(this).apply {
            text = if (showRejected) "Loading rejected anomaly images..." else "Loading confirmed pothole images..."
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 32, 0, 0)
        }
        imageContainer.addView(progress)

        lifecycleScope.launch(Dispatchers.IO) {
            val items = mutableListOf<GalleryItem>()
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            val repo = (application as KhaddaApplication).repository

            if (showRejected) {
                // 1. Load rejected candidates from Room database
                val rejectedCandidates = try {
                    repo.getCandidatesByStatus(CandidateStatus.REJECTED)
                } catch (e: Exception) {
                    emptyList()
                }

                for (cand in rejectedCandidates) {
                    for ((idx, path) in cand.framePaths.withIndex()) {
                        val file = File(path)
                        if (file.exists()) {
                            val timeStr = dateFormat.format(Date(cand.timestampMs))
                            items.add(
                                GalleryItem(
                                    file = file,
                                    title = "REJECTED (Conf: ${String.format(Locale.US, "%.2f", cand.maxConfidence)} | Frame ${idx + 1}/${cand.framePaths.size})",
                                    subtitle = "$timeStr • ID: ${cand.id.take(8)} • Impact: ${String.format(Locale.US, "%.1f", cand.impact)}g",
                                    isConfirmed = false,
                                    candidateId = cand.id
                                )
                            )
                        }
                    }
                }

                // 2. Also load any standalone rejected_pothole_*.jpg files in filesDir
                val standaloneRejected = filesDir.listFiles { f ->
                    f.name.startsWith("rejected_pothole_") && f.name.endsWith(".jpg")
                } ?: emptyArray()

                for (file in standaloneRejected) {
                    if (items.none { it.file.absolutePath == file.absolutePath }) {
                        val timeStr = dateFormat.format(Date(file.lastModified()))
                        items.add(
                            GalleryItem(
                                file = file,
                                title = "REJECTED IMAGE",
                                subtitle = "$timeStr • ${file.name}",
                                isConfirmed = false
                            )
                        )
                    }
                }
            } else {
                // Confirmed potholes
                val confirmedCandidates = try {
                    repo.getCandidatesByStatus(CandidateStatus.CONFIRMED)
                } catch (e: Exception) {
                    emptyList()
                }

                for (cand in confirmedCandidates) {
                    for ((idx, path) in cand.framePaths.withIndex()) {
                        val file = File(path)
                        if (file.exists()) {
                            val timeStr = dateFormat.format(Date(cand.timestampMs))
                            items.add(
                                GalleryItem(
                                    file = file,
                                    title = "CONFIRMED POTHOLE (Conf: ${String.format(Locale.US, "%.2f", cand.maxConfidence)} | Frame ${idx + 1}/${cand.framePaths.size})",
                                    subtitle = "$timeStr • Severity: ${cand.severity} • Impact: ${String.format(Locale.US, "%.1f", cand.impact)}g",
                                    isConfirmed = true,
                                    candidateId = cand.id
                                )
                            )
                        }
                    }
                }

                // Standalone pothole_*.jpg files in filesDir
                val standaloneConfirmed = filesDir.listFiles { f ->
                    f.name.startsWith("pothole_") && f.name.endsWith(".jpg")
                } ?: emptyArray()

                for (file in standaloneConfirmed) {
                    if (items.none { it.file.absolutePath == file.absolutePath }) {
                        val timeStr = dateFormat.format(Date(file.lastModified()))
                        items.add(
                            GalleryItem(
                                file = file,
                                title = "CONFIRMED POTHOLE",
                                subtitle = "$timeStr • ${file.name}",
                                isConfirmed = true
                            )
                        )
                    }
                }
            }

            // Sort by file last modified timestamp (newest first)
            val sortedItems = items.sortedByDescending { it.file.lastModified() }

            withContext(Dispatchers.Main) {
                renderGalleryItems(sortedItems, showRejected)
            }
        }
    }

    private fun renderGalleryItems(items: List<GalleryItem>, showRejected: Boolean) {
        imageContainer.removeAllViews()

        if (items.isEmpty()) {
            val tv = TextView(this).apply {
                text = if (showRejected) {
                    "No rejected images found.\nCandidates rejected by vision verification will be listed here."
                } else {
                    "No confirmed pothole images found."
                }
                setTextColor(Color.LTGRAY)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(32, 64, 32, 0)
            }
            imageContainer.addView(tv)
            return
        }

        val countHeader = TextView(this).apply {
            text = "Total Images: ${items.size}"
            setTextColor(Color.parseColor("#A0A0A0"))
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        imageContainer.addView(countHeader)

        for (item in items) {
            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 24)
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 24)
                }
            }

            // Title Header with Badge
            val titleView = TextView(this).apply {
                text = item.title
                setTextColor(if (item.isConfirmed) Color.parseColor("#4CAF50") else Color.parseColor("#EF5350"))
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }
            cardLayout.addView(titleView)

            // Subtitle
            val subtitleView = TextView(this).apply {
                text = item.subtitle
                setTextColor(Color.parseColor("#B0B0B0"))
                textSize = 12f
                setPadding(0, 4, 0, 12)
            }
            cardLayout.addView(subtitleView)

            // Image Preview
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    700
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.BLACK)

                val bmp = decodeSampledBitmap(item.file.absolutePath, 640, 640)
                if (bmp != null) {
                    setImageBitmap(bmp)
                } else {
                    visibility = View.GONE
                }

                setOnLongClickListener {
                    AlertDialog.Builder(this@DebugGalleryActivity)
                        .setTitle("Delete Image")
                        .setMessage("Delete ${item.file.name}?")
                        .setPositiveButton("Delete") { _, _ ->
                            if (item.file.delete()) {
                                Toast.makeText(this@DebugGalleryActivity, "Image deleted", Toast.LENGTH_SHORT).show()
                                loadGallery(isShowingRejected)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            cardLayout.addView(imageView)

            imageContainer.addView(cardLayout)
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }
}
