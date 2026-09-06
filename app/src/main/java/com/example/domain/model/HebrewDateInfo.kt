package com.example.domain.model

data class HebrewDateInfo(
    val hebrewDay: Int,
    val hebrewMonth: Int,
    val hebrewYear: Int,
    val hebrewDayStr: String, // e.g. כ״ח
    val hebrewMonthNameHe: String, // e.g. תשרי
    val hebrewMonthNameEn: String, // e.g. Tishrei
    val hebrewYearStr: String, // e.g. תשנ״ד or 5754
    val formattedHe: String, // e.g. כ״ח בתשרי תשנ״ד
    val formattedEn: String, // e.g. 28 Tishrei 5754
    val gregorianDay: Int,
    val gregorianMonth: Int, // 1-12
    val gregorianYear: Int,
    val isLeapYear: Boolean,
    /** [java.util.Calendar.DAY_OF_WEEK]: 1 = Sunday. Of the civil date, not the Hebrew one. */
    val dayOfWeek: Int = java.util.Calendar.SUNDAY
) {
    /** e.g. 13/10/1993 — the civil date, as opposed to [formattedEn] which is the Hebrew date in Latin script. */
    val gregorianFormatted: String
        get() = "%02d/%02d/%04d".format(java.util.Locale.US, gregorianDay, gregorianMonth, gregorianYear)
}

data class CalculatedOccurrence(
    val occurrenceIndex: Int,
    val targetHebrewYear: Int,
    val targetHebrewMonth: Int,
    val targetHebrewDay: Int,
    val hebrewDateFormatted: String,
    val gregorianYear: Int,
    val gregorianMonth: Int, // 1-12
    val gregorianDay: Int,
    val gregorianDateFormatted: String,
    val isLeapYear: Boolean,
    /** [java.util.Calendar.DAY_OF_WEEK]: 1 = Sunday. */
    val dayOfWeek: Int = java.util.Calendar.SUNDAY,
    /** Structured, language-free. The UI turns these into text — the engine must not know a locale. */
    val notes: List<OccurrenceNote> = emptyList(),
    /** Set when the origin day does not exist in the target month and was moved. */
    val adjustedFromDay: Int? = null
)

/**
 * Why an occurrence landed where it did. Kept as data rather than a formatted string so the same
 * calculation can be rendered in either language (the previous version emitted Hebrew from the
 * domain layer, which then leaked into English-language calendar entries).
 */
enum class OccurrenceNote {
    LEAP_OBSERVED_IN_ADAR_II,
    LEAP_OBSERVED_IN_ADAR_I,
    LEAP_BOTH_ADAR_I,
    LEAP_BOTH_ADAR_II,
    ORIGIN_ADAR_I_IN_LEAP_YEAR,
    ORIGIN_ADAR_II_IN_LEAP_YEAR,
    COLLAPSED_TO_SINGLE_ADAR,
    CHESHVAN_30_MOVED_TO_KISLEV_1,
    KISLEV_30_MOVED_TO_TEVET_1,
    ADAR_30_MOVED_TO_NISSAN_1,
    DAY_CLAMPED_TO_END_OF_MONTH
}

/**
 * Persisted as its [name] by Room. Parsing is total: an unrecognised value degrades to a
 * sensible default rather than throwing, so a bad row can never crash the app.
 */
enum class EventType {
    BIRTHDAY,
    YAHRZEIT,
    ANNIVERSARY,
    HOLIDAY,
    CUSTOM;

    companion object {
        fun fromId(id: String?): EventType = entries.firstOrNull { it.name == id } ?: CUSTOM
    }
}

enum class RecurrenceType {
    YEARLY,
    MONTHLY,

    /**
     * A single date, not an anniversary.
     *
     * The date entered is the event date itself rather than an origin projected forward, so the
     * leap-year rule and the duration do not apply. For someone who thinks in Hebrew dates but
     * just wants one entry in their calendar.
     */
    ONE_TIME;

    companion object {
        fun fromId(id: String?): RecurrenceType = entries.firstOrNull { it.name == id } ?: YEARLY
    }
}

enum class LeapYearRule {
    STANDARD_ADAR_II, // Halacha default: in a leap year celebrated in Adar II
    ADAR_I,           // In a leap year celebrated in Adar I
    BOTH;             // Celebrated in both Adar I and Adar II (common for Yahrzeits)

    companion object {
        fun fromId(id: String?): LeapYearRule = entries.firstOrNull { it.name == id } ?: STANDARD_ADAR_II
    }
}

/**
 * Reminder offsets for all-day events.
 *
 * An all-day row starts at midnight, and the calendar provider counts reminders *backwards* from
 * the start, so "09:00 on the day itself" is not expressible — 09:00 the day before is 900 minutes
 * back. These are the two offsets that land at a sensible hour.
 */
enum class ReminderOption(val minutes: Int?) {
    NONE(null),
    DAY_BEFORE(900),
    WEEK_BEFORE(900 + 6 * 24 * 60);

    companion object {
        fun fromMinutes(minutes: Int?): ReminderOption =
            entries.firstOrNull { it.minutes == minutes } ?: NONE
    }
}

/**
 * Event colour.
 *
 * The palette is Google Calendar's own eleven event colours, because that is the only set the
 * Calendar API accepts — `colorId` is an index into it, not a free RGB value. Using the same
 * palette everywhere keeps the app, the device calendar and the cloud calendar showing one colour
 * rather than three approximations of it.
 *
 * [DEFAULT] means "whatever the calendar itself uses", which is the behaviour when nothing is
 * chosen.
 */
enum class EventColor(val googleColorId: String?, val argb: Long) {
    DEFAULT(null, 0xFF1A56DB),
    LAVENDER("1", 0xFF7986CB),
    SAGE("2", 0xFF33B679),
    GRAPE("3", 0xFF8E24AA),
    FLAMINGO("4", 0xFFE67C73),
    BANANA("5", 0xFFF6BF26),
    TANGERINE("6", 0xFFF4511E),
    PEACOCK("7", 0xFF039BE5),
    GRAPHITE("8", 0xFF616161),
    BLUEBERRY("9", 0xFF3F51B5),
    BASIL("10", 0xFF0B8043),
    TOMATO("11", 0xFFD50000);

    companion object {
        fun fromId(id: String?): EventColor = entries.firstOrNull { it.name == id } ?: DEFAULT
    }
}
