package com.pothole.khadda.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pothole.khadda.model.CandidateStatus
import com.pothole.khadda.model.RepairStatus
import com.pothole.khadda.model.SessionStatus
import com.pothole.khadda.model.SeverityLevel

class Converters {
    @TypeConverter
    fun fromSeverity(value: SeverityLevel): String = value.name

    @TypeConverter
    fun toSeverity(value: String): SeverityLevel =
        try { SeverityLevel.valueOf(value) } catch (e: Exception) { SeverityLevel.MEDIUM }

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus =
        try { SessionStatus.valueOf(value) } catch (e: Exception) { SessionStatus.STOPPED }

    @TypeConverter
    fun fromRepairStatus(value: RepairStatus): String = value.name

    @TypeConverter
    fun toRepairStatus(value: String): RepairStatus =
        try { RepairStatus.valueOf(value) } catch (e: Exception) { RepairStatus.REPORTED }

    @TypeConverter
    fun fromCandidateStatus(value: CandidateStatus): String = value.name

    @TypeConverter
    fun toCandidateStatus(value: String): CandidateStatus =
        try { CandidateStatus.valueOf(value) } catch (e: Exception) { CandidateStatus.PENDING }

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        Gson().toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
