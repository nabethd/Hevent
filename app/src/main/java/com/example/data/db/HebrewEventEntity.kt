package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hebrew_events")
data class HebrewEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val eventType: String = "BIRTHDAY", // BIRTHDAY, YAHRZEIT, ANNIVERSARY, HOLIDAY, CUSTOM
    val recurrenceType: String = "YEARLY", // YEARLY, MONTHLY
    // Hebrew source date
    val hebrewDay: Int,
    val hebrewMonth: Int, // KosherJava month constant (1 = Nissan ... 7 = Tishrei ... 12 = Adar/Adar I, 13 = Adar II)
    val hebrewYear: Int, // e.g. 5754
    val hebrewDateFormatted: String, // e.g. "כ״ח בתשרי תשנ״ד"
    // Gregorian source date
    val gregorianDay: Int,
    val gregorianMonth: Int, // 1-12
    val gregorianYear: Int, // e.g. 1993
    // Halachic Leap year behavior: "STANDARD_ADAR_II", "ADAR_I", "BOTH"
    val leapYearRule: String = "STANDARD_ADAR_II",
    // Number of occurrences calculated (default 100)
    val yearsCount: Int = 100,
    // Sync information
    val targetCalendarId: Long? = null,
    val targetCalendarName: String? = null,
    val isSyncedToCalendar: Boolean = false,
    val syncedEventsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
