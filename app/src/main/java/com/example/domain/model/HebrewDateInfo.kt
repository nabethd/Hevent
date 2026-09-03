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
    val isLeapYear: Boolean
)

data class CalculatedOccurrence(
    val occurrenceIndex: Int, // 1 to 100
    val targetHebrewYear: Int,
    val targetHebrewMonth: Int,
    val targetHebrewDay: Int,
    val hebrewDateFormatted: String,
    val gregorianYear: Int,
    val gregorianMonth: Int, // 1-12
    val gregorianDay: Int,
    val gregorianDateFormatted: String,
    val isLeapYear: Boolean,
    val note: String? = null
)

enum class LeapYearRule(val id: String) {
    STANDARD_ADAR_II("STANDARD_ADAR_II"), // Halacha default: in leap year celebrated in Adar II
    ADAR_I("ADAR_I"),                     // In leap year celebrated in Adar I
    BOTH("BOTH")                          // Celebrated in both Adar I and Adar II (common for Yahrzeits)
}
