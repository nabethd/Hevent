package com.example.domain.hebrew

import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.LeapYearRule
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.Calendar

object HebrewCalendarEngine {

    private val hebrewFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
    }

    private val englishFormatter = HebrewDateFormatter().apply {
        isHebrewFormat = false
    }

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
     * Converts a Gregorian date (year, 1-based month, day) to a rich HebrewDateInfo.
     */
    fun fromGregorian(year: Int, month: Int, day: Int): HebrewDateInfo {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val jd = JewishDate(cal.time)
        return toDateInfo(jd, year, month, day)
    }

    /**
     * Converts a Hebrew date (year, month, day) to a rich HebrewDateInfo.
     */
    fun fromHebrew(hebrewYear: Int, hebrewMonth: Int, hebrewDay: Int): HebrewDateInfo {
        val jd = JewishDate()
        // Clamp day to valid days in month
        val maxDays = getDaysInMonth(hebrewYear, hebrewMonth)
        val clampedDay = hebrewDay.coerceIn(1, maxDays)
        jd.setJewishDate(hebrewYear, hebrewMonth, clampedDay)

        val gYear = jd.gregorianYear
        val gMonth = jd.gregorianMonth + 1 // 0-based to 1-based
        val gDay = jd.gregorianDayOfMonth
        return toDateInfo(jd, gYear, gMonth, gDay)
    }

    /**
     * Returns today's Hebrew and Gregorian info.
     */
    fun getToday(): HebrewDateInfo {
        val cal = Calendar.getInstance()
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        return fromGregorian(gYear, gMonth, gDay)
    }

    /**
     * Get maximum days in a Hebrew month for a given Hebrew year.
     */
    fun getDaysInMonth(hebrewYear: Int, hebrewMonth: Int): Int {
        val jd = JewishDate()
        jd.setJewishDate(hebrewYear, hebrewMonth, 1)
        return jd.daysInJewishMonth
    }

    /**
     * Check if a Jewish year is a leap year (13 months).
     */
    fun isLeapYear(hebrewYear: Int): Boolean {
        val jd = JewishDate()
        jd.setJewishDate(hebrewYear, JewishDate.TISHREI, 1)
        return jd.isJewishLeapYear
    }

    /**
     * Formats a Hebrew number (e.g. 28 -> כ״ח).
     */
    fun formatHebrewNumber(num: Int): String {
        return hebrewFormatter.formatHebrewNumber(num)
    }

    /**
     * Calculates occurrences for the next [yearsCount] years (default 100).
     *
     * Correctly handles:
     * - Leap years (Adar I vs Adar II based on origin date and selected Halachic rule).
     * - Short months: 30 Cheshvan and 30 Kislev in defective years.
     * - 30 Adar I in regular years.
     */
    fun calculateYearlyOccurrences(
        originHebrewYear: Int,
        originHebrewMonth: Int,
        originHebrewDay: Int,
        leapYearRule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II,
        yearsCount: Int = 100,
        startFromCurrentYear: Boolean = true
    ): List<CalculatedOccurrence> {
        val results = mutableListOf<CalculatedOccurrence>()
        val todayJd = JewishDate()
        val currentHYear = todayJd.jewishYear

        val startYear = if (startFromCurrentYear) currentHYear else originHebrewYear
        val endYear = startYear + yearsCount - 1

        val originWasLeap = isLeapYear(originHebrewYear)
        val originWasAdarRegular = !originWasLeap && originHebrewMonth == JewishDate.ADAR
        val originWasAdarI = originWasLeap && originHebrewMonth == JewishDate.ADAR
        val originWasAdarII = originWasLeap && originHebrewMonth == JewishDate.ADAR_II

        var index = 1

        for (targetYear in startYear..endYear) {
            val targetIsLeap = isLeapYear(targetYear)

            // Determine target month(s)
            val targetMonths = mutableListOf<Pair<Int, String?>>() // Pair of (month, note)

            when {
                // Case 1: Origin was regular year Adar
                originWasAdarRegular -> {
                    if (targetIsLeap) {
                        when (leapYearRule) {
                            LeapYearRule.STANDARD_ADAR_II -> {
                                targetMonths.add(Pair(JewishDate.ADAR_II, "שנה מעוברת: נחגג באדר ב׳ (הלכה)"))
                            }
                            LeapYearRule.ADAR_I -> {
                                targetMonths.add(Pair(JewishDate.ADAR, "שנה מעוברת: נחגג באדר א׳"))
                            }
                            LeapYearRule.BOTH -> {
                                targetMonths.add(Pair(JewishDate.ADAR, "שנה מעוברת: אדר א׳ (שני האדרים)"))
                                targetMonths.add(Pair(JewishDate.ADAR_II, "שנה מעוברת: אדר ב׳ (שני האדרים)"))
                            }
                        }
                    } else {
                        targetMonths.add(Pair(JewishDate.ADAR, null))
                    }
                }

                // Case 2: Origin was Adar I of a leap year
                originWasAdarI -> {
                    if (targetIsLeap) {
                        targetMonths.add(Pair(JewishDate.ADAR, "שנה מעוברת: אדר א׳"))
                    } else {
                        targetMonths.add(Pair(JewishDate.ADAR, "שנה פשוטה: אדר"))
                    }
                }

                // Case 3: Origin was Adar II of a leap year
                originWasAdarII -> {
                    if (targetIsLeap) {
                        targetMonths.add(Pair(JewishDate.ADAR_II, "שנה מעוברת: אדר ב׳"))
                    } else {
                        targetMonths.add(Pair(JewishDate.ADAR, "שנה פשוטה: אדר"))
                    }
                }

                // Case 4: Any other standard Hebrew month
                else -> {
                    targetMonths.add(Pair(originHebrewMonth, null))
                }
            }

            for ((tMonth, ruleNote) in targetMonths) {
                // Handle short month edge cases
                val daysInTargetMonth = getDaysInMonth(targetYear, tMonth)
                var targetDay = originHebrewDay
                var targetMonthFinal = tMonth
                var targetYearFinal = targetYear
                var edgeCaseNote: String? = null

                if (originHebrewDay > daysInTargetMonth) {
                    when {
                        // 30 Cheshvan in a year with only 29 days -> celebrated 1 Kislev
                        tMonth == JewishDate.CHESHVAN && originHebrewDay == 30 -> {
                            targetMonthFinal = JewishDate.KISLEV
                            targetDay = 1
                            edgeCaseNote = "ל' חשוון חסר -> חל בא' כסלו"
                        }
                        // 30 Kislev in a year with only 29 days -> celebrated 1 Tevet
                        tMonth == JewishDate.KISLEV && originHebrewDay == 30 -> {
                            targetMonthFinal = JewishDate.TEVES
                            targetDay = 1
                            edgeCaseNote = "ל' כסלו חסר -> חל בא' טבת (נר ו' חנוכה)"
                        }
                        // 30 Adar I in a regular year where Adar only has 29 days -> 1 Nissan
                        tMonth == JewishDate.ADAR && originHebrewDay == 30 -> {
                            targetMonthFinal = JewishDate.NISSAN
                            targetDay = 1
                            edgeCaseNote = "ל' אדר חסר -> חל בא' ניסן"
                        }
                        else -> {
                            targetDay = daysInTargetMonth
                            edgeCaseNote = "יום ${originHebrewDay} לא קיים -> הותאם ליום האחרון בחודש (${daysInTargetMonth})"
                        }
                    }
                }

                val jdTarget = JewishDate()
                jdTarget.setJewishDate(targetYearFinal, targetMonthFinal, targetDay)

                val gYear = jdTarget.gregorianYear
                val gMonth = jdTarget.gregorianMonth + 1
                val gDay = jdTarget.gregorianDayOfMonth

                val combinedNote = listOfNotNull(ruleNote, edgeCaseNote).joinToString(" • ").ifEmpty { null }

                val formattedHebrew = buildFormattedHebrewDate(targetDay, targetMonthFinal, targetYearFinal, targetIsLeap)
                val formattedGregorian = String.format("%02d/%02d/%04d", gDay, gMonth, gYear)

                results.add(
                    CalculatedOccurrence(
                        occurrenceIndex = index++,
                        targetHebrewYear = targetYearFinal,
                        targetHebrewMonth = targetMonthFinal,
                        targetHebrewDay = targetDay,
                        hebrewDateFormatted = formattedHebrew,
                        gregorianYear = gYear,
                        gregorianMonth = gMonth,
                        gregorianDay = gDay,
                        gregorianDateFormatted = formattedGregorian,
                        isLeapYear = targetIsLeap,
                        note = combinedNote
                    )
                )
            }
        }

        return results
    }

    /**
     * Calculates occurrences for monthly recurrence (e.g. Rosh Chodesh or recurring day of month)
     * for [monthsCount] months ahead.
     */
    fun calculateMonthlyOccurrences(
        originHebrewDay: Int,
        monthsCount: Int = 120 // 10 years by default
    ): List<CalculatedOccurrence> {
        val results = mutableListOf<CalculatedOccurrence>()
        val today = JewishDate()
        var currentYear = today.jewishYear
        var currentMonth = today.jewishMonth

        for (i in 1..monthsCount) {
            val isLeap = isLeapYear(currentYear)
            val daysInM = getDaysInMonth(currentYear, currentMonth)
            val targetDay = originHebrewDay.coerceIn(1, daysInM)

            val jd = JewishDate()
            jd.setJewishDate(currentYear, currentMonth, targetDay)

            val gYear = jd.gregorianYear
            val gMonth = jd.gregorianMonth + 1
            val gDay = jd.gregorianDayOfMonth

            val note = if (originHebrewDay > daysInM) {
                "חודש חסר: הותאם ליום ${targetDay}"
            } else null

            results.add(
                CalculatedOccurrence(
                    occurrenceIndex = i,
                    targetHebrewYear = currentYear,
                    targetHebrewMonth = currentMonth,
                    targetHebrewDay = targetDay,
                    hebrewDateFormatted = buildFormattedHebrewDate(targetDay, currentMonth, currentYear, isLeap),
                    gregorianYear = gYear,
                    gregorianMonth = gMonth,
                    gregorianDay = gDay,
                    gregorianDateFormatted = String.format("%02d/%02d/%04d", gDay, gMonth, gYear),
                    isLeapYear = isLeap,
                    note = note
                )
            )

            // Advance to next Jewish month
            if (isLeap) {
                if (currentMonth == JewishDate.ADAR_II) {
                    currentMonth = JewishDate.NISSAN
                } else if (currentMonth == JewishDate.ELUL) {
                    currentMonth = JewishDate.TISHREI
                    currentYear++
                } else {
                    currentMonth++
                }
            } else {
                if (currentMonth == JewishDate.ADAR) {
                    currentMonth = JewishDate.NISSAN
                } else if (currentMonth == JewishDate.ELUL) {
                    currentMonth = JewishDate.TISHREI
                    currentYear++
                } else {
                    currentMonth++
                }
            }
        }
        return results
    }

    private fun toDateInfo(jd: JewishDate, gYear: Int, gMonth: Int, gDay: Int): HebrewDateInfo {
        val hDay = jd.jewishDayOfMonth
        val hMonth = jd.jewishMonth
        val hYear = jd.jewishYear
        val isLeap = jd.isJewishLeapYear

        val dayStr = hebrewFormatter.formatHebrewNumber(hDay)
        val monthNameHe = getHebrewMonthName(hMonth, isLeap, isHebrew = true)
        val monthNameEn = getHebrewMonthName(hMonth, isLeap, isHebrew = false)
        val yearStr = hebrewFormatter.formatHebrewNumber(hYear)

        val formattedHe = "$dayStr ב$monthNameHe $yearStr"
        val formattedEn = "$hDay $monthNameEn $hYear"

        return HebrewDateInfo(
            hebrewDay = hDay,
            hebrewMonth = hMonth,
            hebrewYear = hYear,
            hebrewDayStr = dayStr,
            hebrewMonthNameHe = monthNameHe,
            hebrewMonthNameEn = monthNameEn,
            hebrewYearStr = yearStr,
            formattedHe = formattedHe,
            formattedEn = formattedEn,
            gregorianDay = gDay,
            gregorianMonth = gMonth,
            gregorianYear = gYear,
            isLeapYear = isLeap
        )
    }

    fun getHebrewMonthName(month: Int, isLeap: Boolean, isHebrew: Boolean): String {
        return if (isHebrew) {
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
    }

    private fun buildFormattedHebrewDate(day: Int, month: Int, year: Int, isLeap: Boolean): String {
        val dayStr = hebrewFormatter.formatHebrewNumber(day)
        val monthStr = getHebrewMonthName(month, isLeap, isHebrew = true)
        val yearStr = hebrewFormatter.formatHebrewNumber(year)
        return "$dayStr ב$monthStr $yearStr"
    }
}
