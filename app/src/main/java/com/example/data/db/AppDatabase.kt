package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HebrewEventEntity::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hebrewEventDao(): HebrewEventDao

    companion object {
        /**
         * v1 -> v2
         *  - `syncTag`: per-event marker so calendar rows can be deleted precisely instead of by title.
         *  - `occurrenceCount`: v1 stored the *occurrence* count in `yearsCount`, which meant every
         *    re-export projected further into the future than the user asked for. Split the two and
         *    recover `yearsCount` where the original value is derivable (monthly rows were years * 12).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hebrew_events ADD COLUMN syncTag TEXT")
                db.execSQL("ALTER TABLE hebrew_events ADD COLUMN occurrenceCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE hebrew_events SET occurrenceCount = yearsCount")
                db.execSQL(
                    "UPDATE hebrew_events SET yearsCount = MAX(1, yearsCount / 12) " +
                        "WHERE recurrenceType = 'MONTHLY'"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_hebrew_events_title ON hebrew_events (title)")
            }
        }

        /** v2 -> v3: sunset handling and per-event reminders. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hebrew_events ADD COLUMN afterSunset INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hebrew_events ADD COLUMN reminderMinutes INTEGER")
            }
        }

        /** v3 -> v4: remember the Google Calendar an event was synced to, so it can be undone. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hebrew_events ADD COLUMN cloudCalendarId TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Double-checked locking: the second read inside the lock is what makes this correct.
            // Without it two threads can both build a database and the second silently wins.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hebrew_calendar_sync.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
