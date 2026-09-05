package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.EventType
import com.example.domain.model.LeapYearRule
import com.example.domain.model.RecurrenceType

@Entity(
    tableName = "hebrew_events",
    indices = [Index("title")]
)
data class HebrewEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val eventType: EventType = EventType.BIRTHDAY,
    val recurrenceType: RecurrenceType = RecurrenceType.YEARLY,
    // Hebrew source date
    val hebrewDay: Int,
    val hebrewMonth: Int, // KosherJava month constant (1 = Nissan ... 7 = Tishrei ... 12 = Adar/Adar I, 13 = Adar II)
    val hebrewYear: Int, // e.g. 5754
    val hebrewDateFormatted: String, // e.g. "כ״ח בתשרי תשנ״ד"
    // Gregorian source date
    val gregorianDay: Int,
    val gregorianMonth: Int, // 1-12
    val gregorianYear: Int, // e.g. 1993
    // Halachic leap year behaviour
    val leapYearRule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II,
    /** How many years forward the user asked us to project. NOT the number of occurrences. */
    val yearsCount: Int = 20,
    /** How many occurrences that projection actually produced (BOTH-Adar years yield two). */
    val occurrenceCount: Int = 0,
    /**
     * Unique marker written into every calendar row this event created, as an
     * ExtendedProperties value and inside the description. Deletion matches on this and
     * never on the title alone, so we can never remove an event the user created themselves.
     * Null for rows written before this field existed.
     */
    val syncTag: String? = null,
    // Sync information
    val targetCalendarId: Long? = null,
    val targetCalendarName: String? = null,
    val isSyncedToCalendar: Boolean = false,
    val syncedEventsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
