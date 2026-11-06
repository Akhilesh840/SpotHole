package com.pothole.khadda.utils

import android.content.Context
import com.pothole.khadda.detector.PotholeVisionAnalyzer
import java.security.MessageDigest

/**
 * Utility for extracting model file metadata and computing cryptographic hashes.
 */
object ModelMetadataUtil {

    /**
     * Computes the SHA-256 hex string of the model file from Android assets.
     */
    fun getModelSha256(
        context: Context,
        modelFileName: String = PotholeVisionAnalyzer.DEFAULT_MODEL_FILE
    ): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open(modelFileName).use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unavailable: ${e.message}"
        }
    }
}
