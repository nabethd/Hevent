package com.example.domain.hebrew

import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.LeapYearRule
import com.example.domain.model.OccurrenceNote
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.Calendar
import java.util.Locale

object HebrewCalendarEngine {

    private val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    val HEBREW_MONTH_NAMES_HE = mapOf(
        JewishDate.NISSAN to "ניסן",
        JewishDate.IYAR to "אייר",
        JewishDate.SIVAN to "סיוון",
        JewishDate.TAMMUZ to "תמוז",
        JewishDate.AV to "אב",
        JewishDate.ELUL to "אלול",
        JewishDate.TISHREI to "תשרי",
        JewishDate.CHESHVAN to "חשוון",
        JewishDate.KISLEV to "כסלו",
        JewishDate.TEVES to "טבת",
        JewishDate.SHEVAT to "שבט",
        JewishDate.ADAR to "אדר",
        JewishDate.ADAR_II to "אדר ב׳"
    )

    val HEBREW_MONTH_NAMES_EN = mapOf(
        JewishDate.NISSAN to "Nissan",
        JewishDate.IYAR to "Iyar",
        JewishDate.SIVAN to "Sivan",
        JewishDate.TAMMUZ to "Tammuz",
        JewishDate.AV to "Av",
        JewishDate.ELUL to "Elul",
        JewishDate.TISHREI to "Tishrei",
        JewishDate.CHESHVAN to "Cheshvan",
        JewishDate.KISLEV to "Kislev",
        JewishDate.TEVES to "Tevet",
        JewishDate.SHEVAT to "Shevat",
        JewishDate.ADAR to "Adar",
        JewishDate.ADAR_II to "Adar II"
    )

    /**
     * Adar II only exists in a leap year. A picker can leave the month on Adar II while the year
     * moves to a regular one; KosherJava rejects that pair, so every entry point normalises first
     * rather than letting an invalid pair reach [JewishDate.setJewishDate].
     */
    fun normalizeMonth(hebrewYear: Int, hebrewMonth: Int): Int = when {
        hebrewMonth == JewishDate.ADAR_II && !isLeapYear(hebrewYear) -> JewishDate.ADAR
        hebrewMonth < JewishDate.NISSAN || hebrewMonth > JewishDate.ADAR_II -> JewishDate.TISHREI
        else -> hebrewMonth
    }

    /** Converts a Gregorian date (1-based month) to a rich [HebrewDateInfo]. */
    fun fromGregorian(year: Int, month: Int, day: Int): HebrewDateInfo {
        // Noon avoids any DST/rounding edge; the Hebrew day boundary is sunset, which the caller
        // accounts for separately.
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day.coerceIn(1, 31), 12, 0, 0)
        }
        val jd = JewishDate(cal.time)
        return toDateInfo(
            jd,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Converts a Hebrew date to a rich [HebrewDateInfo], clamping day and month into range. */
    fun fromHebrew(hebrewYear: Int, hebrewMonth: Int, hebrewDay: Int): HebrewDateInfo {
        val month = normalizeMonth(hebrewYear, hebrewMonth)
        val jd = JewishDate()
        jd.setJewishDate(hebrewYear, month, hebrewDay.coerceIn(1, getDaysInMonth(hebrewYear, month)))
        return toDateInfo(jd, jd.gregorianYear, jd.gregorianMonth + 1, jd.gregorianDayOfMonth)
    }

    fun getToday(): HebrewDateInfo {
        val cal = Calendar.getInstance()
        return fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Number of days in a Hebrew month, tolerating an out-of-range month. */
    fun getDaysInMonth(hebrewYear: Int, hebrewMonth: Int): Int {
        val jd = JewishDate()
        jd.setJewishDate(hebrewYear, normalizeMonth(hebrewYear, hebrewMonth), 1)
        return jd.daysInJewishMonth
    }

    fun isLeapYear(hebrewYear: Int): Boolean {
        val jd = JewishDate()
        jd.setJewishDate(hebrewYear, JewishDate.TISHREI, 1)
        return jd.isJewishLeapYear
    }

    fun formatHebrewNumber(num: Int): String = hebrewFormatter.formatHebrewNumber(num)

    /**
     * Projects a yearly Hebrew anniversary forward.
     *
     * Handles leap years (Adar I vs Adar II, per [leapYearRule]), defective Cheshvan and Kislev,
     * and 30 Adar I in a regular year.
     *
     * Occurrences that already passed are dropped, so the returned list always starts with the
     * *next* one. Without this the first entry sat in the past for most of the year and the UI
     * rendered it as "today".
     *
     * @return exactly [yearsCount] Hebrew years' worth of occurrences (a BOTH-Adar rule yields two
     *         in leap years, so the list can be longer than [yearsCount]).
     */
    fun calculateYearlyOccurrences(
        originHebrewYear: Int,
        originHebrewMonth: Int,
        originHebrewDay: Int,
        leapYearRule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II,
        yearsCount: Int = 20,
        includePast: Boolean = false
    ): List<CalculatedOccurrence> {
        val originMonth = normalizeMonth(originHebrewYear, originHebrewMonth)
        val todayMillis = startOfToday()
        val firstYear = JewishDate().jewishYear

        val originWasLeap = isLeapYear(originHebrewYear)
        val originWasAdarRegular = !originWasLeap && originMonth == JewishDate.ADAR
        val originWasAdarI = originWasLeap && originMonth == JewishDate.ADAR
        val originWasAdarII = originWasLeap && originMonth == JewishDate.ADAR_II

        val collected = projectYears(
            originMonth = originMonth,
            originDay = originHebrewDay,
            originWasAdarRegular = originWasAdarRegular,
            originWasAdarI = originWasAdarI,
            originWasAdarII = originWasAdarII,
            leapYearRule = leapYearRule,
            // One extra year of headroom so dropping past occurrences still leaves a full span.
            years = firstYear..(firstYear + yearsCount)
        )

        val future = if (includePast) collected else collected.filter { gregorianMillis(it) >= todayMillis }
        val keptYears = future.map { it.targetHebrewYear }.distinct().take(yearsCount).toSet()

        return future.filter { it.targetHebrewYear in keptYears }
            .mapIndexed { i, occ -> occ.copy(occurrenceIndex = i + 1) }
    }

    /** Projects a monthly Hebrew recurrence forward, skipping months that already passed. */
    fun calculateMonthlyOccurrences(
        originHebrewDay: Int,
        monthsCount: Int = 120,
        includePast: Boolean = false
    ): List<CalculatedOccurrence> {
        val todayMillis = startOfToday()
        val today = JewishDate()
        var year = today.jewishYear
        var month = today.jewishMonth

        val collected = mutableListOf<CalculatedOccurrence>()
        // Headroom of one month, for when this month's day has already passed.
        repeat(monthsCount + 1) {
            collected += buildOccurrence(year, month, originHebrewDay, isLeapYear(year), null)

            val lastMonthOfYear = if (isLeapYear(year)) JewishDate.ADAR_II else JewishDate.ADAR
            when (month) {
                lastMonthOfYear -> month = JewishDate.NISSAN
                JewishDate.ELUL -> { month = JewishDate.TISHREI; year++ }
                else -> month++
            }
        }

        val future = if (includePast) collected else collected.filter { gregorianMillis(it) >= todayMillis }
        return future.take(monthsCount).mapIndexed { i, occ -> occ.copy(occurrenceIndex = i + 1) }
    }

    /**
     * Occurrences falling inside an explicit span of Hebrew years, past ones included.
     *
     * The calendar screen needs this: it browses arbitrary months, whereas
     * [calculateYearlyOccurrences] deliberately starts at the current year. Both go through the
     * same [projectYears], so the grid can no longer disagree with what was actually synced —
     * previously the screen re-implemented the Adar and short-month rules by hand and got them
     * wrong.
     */
    fun occurrencesInHebrewYears(
        originHebrewYear: Int,
        originHebrewMonth: Int,
        originHebrewDay: Int,
        leapYearRule: LeapYearRule,
        years: IntRange
    ): List<CalculatedOccurrence> {
        val originMonth = normalizeMonth(originHebrewYear, originHebrewMonth)
        val originWasLeap = isLeapYear(originHebrewYear)
        return projectYears(
            originMonth = originMonth,
            originDay = originHebrewDay,
            originWasAdarRegular = !originWasLeap && originMonth == JewishDate.ADAR,
            originWasAdarI = originWasLeap && originMonth == JewishDate.ADAR,
            originWasAdarII = originWasLeap && originMonth == JewishDate.ADAR_II,
            leapYearRule = leapYearRule,
            years = years
        ).mapIndexed { i, occ -> occ.copy(occurrenceIndex = i + 1) }
    }

    /** Which Hebrew day a monthly recurrence lands on in the given month, after clamping. */
    fun monthlyDayIn(hebrewYear: Int, hebrewMonth: Int, originDay: Int): Int =
        originDay.coerceIn(1, getDaysInMonth(hebrewYear, hebrewMonth))

    private fun projectYears(
        originMonth: Int,
        originDay: Int,
        originWasAdarRegular: Boolean,
        originWasAdarI: Boolean,
        originWasAdarII: Boolean,
        leapYearRule: LeapYearRule,
        years: IntRange
    ): List<CalculatedOccurrence> {
        val collected = mutableListOf<CalculatedOccurrence>()
        for (targetYear in years) {
            val targetIsLeap = isLeapYear(targetYear)

            val targetMonths: List<Pair<Int, OccurrenceNote?>> = when {
                originWasAdarRegular -> if (!targetIsLeap) {
                    listOf(JewishDate.ADAR to null)
                } else when (leapYearRule) {
                    LeapYearRule.STANDARD_ADAR_II ->
                        listOf(JewishDate.ADAR_II to OccurrenceNote.LEAP_OBSERVED_IN_ADAR_II)
                    LeapYearRule.ADAR_I ->
                        listOf(JewishDate.ADAR to OccurrenceNote.LEAP_OBSERVED_IN_ADAR_I)
                    LeapYearRule.BOTH -> listOf(
                        JewishDate.ADAR to OccurrenceNote.LEAP_BOTH_ADAR_I,
                        JewishDate.ADAR_II to OccurrenceNote.LEAP_BOTH_ADAR_II
                    )
                }

                originWasAdarI -> listOf(
                    JewishDate.ADAR to
                        if (targetIsLeap) OccurrenceNote.ORIGIN_ADAR_I_IN_LEAP_YEAR
                        else OccurrenceNote.COLLAPSED_TO_SINGLE_ADAR
                )

                originWasAdarII -> if (targetIsLeap) {
                    listOf(JewishDate.ADAR_II to OccurrenceNote.ORIGIN_ADAR_II_IN_LEAP_YEAR)
                } else {
                    listOf(JewishDate.ADAR to OccurrenceNote.COLLAPSED_TO_SINGLE_ADAR)
                }

                else -> listOf(originMonth to null)
            }

            for ((month, ruleNote) in targetMonths) {
                collected += buildOccurrence(targetYear, month, originDay, targetIsLeap, ruleNote)
            }
        }
        return collected
    }

    private fun buildOccurrence(
        targetYear: Int,
        targetMonth: Int,
        originDay: Int,
        targetIsLeap: Boolean,
        ruleNote: OccurrenceNote?
    ): CalculatedOccurrence {
        val daysInMonth = getDaysInMonth(targetYear, targetMonth)
        var month = targetMonth
        var day = originDay
        var edgeNote: OccurrenceNote? = null
        var adjustedFrom: Int? = null

        if (originDay > daysInMonth) {
            adjustedFrom = originDay
            when {
                targetMonth == JewishDate.CHESHVAN && originDay == 30 -> {
                    month = JewishDate.KISLEV; day = 1
                    edgeNote = OccurrenceNote.CHESHVAN_30_MOVED_TO_KISLEV_1
                }
                targetMonth == JewishDate.KISLEV && originDay == 30 -> {
                    month = JewishDate.TEVES; day = 1
                    edgeNote = OccurrenceNote.KISLEV_30_MOVED_TO_TEVET_1
                }
                targetMonth == JewishDate.ADAR && originDay == 30 -> {
                    month = JewishDate.NISSAN; day = 1
                    edgeNote = OccurrenceNote.ADAR_30_MOVED_TO_NISSAN_1
                }
                else -> {
                    day = daysInMonth
                    edgeNote = OccurrenceNote.DAY_CLAMPED_TO_END_OF_MONTH
                }
            }
        }

        val jd = JewishDate()
        jd.setJewishDate(targetYear, month, day)
        val gYear = jd.gregorianYear
        val gMonth = jd.gregorianMonth + 1
        val gDay = jd.gregorianDayOfMonth

        return CalculatedOccurrence(
            occurrenceIndex = 0, // assigned after past occurrences are filtered out
            targetHebrewYear = targetYear,
            targetHebrewMonth = month,
            targetHebrewDay = day,
            hebrewDateFormatted = buildFormattedHebrewDate(day, month, targetYear, targetIsLeap),
            gregorianYear = gYear,
            gregorianMonth = gMonth,
            gregorianDay = gDay,
            gregorianDateFormatted = String.format(Locale.US, "%02d/%02d/%04d", gDay, gMonth, gYear),
            isLeapYear = targetIsLeap,
            notes = listOfNotNull(ruleNote, edgeNote),
            adjustedFromDay = adjustedFrom
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun gregorianMillis(occ: CalculatedOccurrence): Long = Calendar.getInstance().apply {
        clear()
        set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
    }.timeInMillis

    private fun toDateInfo(jd: JewishDate, gYear: Int, gMonth: Int, gDay: Int): HebrewDateInfo {
        val hDay = jd.jewishDayOfMonth
        val hMonth = jd.jewishMonth
        val hYear = jd.jewishYear
        val isLeap = jd.isJewishLeapYear

        val dayStr = hebrewFormatter.formatHebrewNumber(hDay)
        val monthNameHe = getHebrewMonthName(hMonth, isLeap, isHebrew = true)
        val monthNameEn = getHebrewMonthName(hMonth, isLeap, isHebrew = false)
        val yearStr = hebrewFormatter.formatHebrewNumber(hYear)

        return HebrewDateInfo(
            hebrewDay = hDay,
            hebrewMonth = hMonth,
            hebrewYear = hYear,
            hebrewDayStr = dayStr,
            hebrewMonthNameHe = monthNameHe,
            hebrewMonthNameEn = monthNameEn,
            hebrewYearStr = yearStr,
            formattedHe = "$dayStr ב$monthNameHe $yearStr",
            formattedEn = "$hDay $monthNameEn $hYear",
            gregorianDay = gDay,
            gregorianMonth = gMonth,
            gregorianYear = gYear,
            isLeapYear = isLeap
        )
    }

    fun getHebrewMonthName(month: Int, isLeap: Boolean, isHebrew: Boolean): String = if (isHebrew) {
        when {
            month == JewishDate.ADAR && isLeap -> "אדר א׳"
            month == JewishDate.ADAR_II -> "אדר ב׳"
            else -> HEBREW_MONTH_NAMES_HE[month] ?: "חודש $month"
        }
    } else {
        when {
            month == JewishDate.ADAR && isLeap -> "Adar I"
            month == JewishDate.ADAR_II -> "Adar II"
            else -> HEBREW_MONTH_NAMES_EN[month] ?: "Month $month"
        }
    }

    private fun buildFormattedHebrewDate(day: Int, month: Int, year: Int, isLeap: Boolean): String {
        val dayStr = hebrewFormatter.formatHebrewNumber(day)
        val monthStr = getHebrewMonthName(month, isLeap, isHebrew = true)
        val yearStr = hebrewFormatter.formatHebrewNumber(year)
        return "$dayStr ב$monthStr $yearStr"
    }
}
