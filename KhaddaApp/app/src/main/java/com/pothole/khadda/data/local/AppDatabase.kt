package com.pothole.khadda.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pothole.khadda.model.CandidateEvent
import com.pothole.khadda.model.DetectionSession
import com.pothole.khadda.model.PotholeEvent

/**
 * Local Room Database implementation with version migrations.
 * Database name: "khadda_pothole.db".
 */
@Database(
    entities = [PotholeEvent::class, DetectionSession::class, CandidateEvent::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun potholeDao(): PotholeDao
    abstract fun sessionDao(): SessionDao
    abstract fun candidateDao(): CandidateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2:
         * - Adds candidateId column to pothole_events
         * - Creates candidate_events table and corresponding indexes
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add candidateId column to existing pothole_events table
                db.execSQL("ALTER TABLE pothole_events ADD COLUMN candidateId TEXT DEFAULT NULL")

                // 2. Create candidate_events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS candidate_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        sessionId TEXT NOT NULL,
                        timestampMs INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        speedMps REAL NOT NULL,
                        triggeredZDiff INTEGER NOT NULL,
                        triggeredZThresh INTEGER NOT NULL,
                        triggeredStdev INTEGER NOT NULL,
                        triggeredGZero INTEGER NOT NULL,
                        zDiff REAL NOT NULL,
                        zThresh REAL NOT NULL,
                        stdev REAL NOT NULL,
                        impact REAL NOT NULL,
                        severity TEXT NOT NULL,
                        status TEXT NOT NULL,
                        maxConfidence REAL NOT NULL,
                        framePaths TEXT NOT NULL,
                        inferenceMsPerFrame INTEGER NOT NULL,
                        modelFileName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // 3. Create indexes for fast session and status queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_candidate_events_sessionId ON candidate_events(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_candidate_events_status ON candidate_events(status)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "khadda_pothole.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
