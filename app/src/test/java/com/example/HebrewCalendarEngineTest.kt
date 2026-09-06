package com.example

import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.model.LeapYearRule
import com.example.domain.model.OccurrenceNote
import com.example.domain.model.ReminderOption
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the rules that make this app non-trivial: Adar I vs Adar II, defective Cheshvan and
 * Kislev, and 30 Adar in a regular year.
 *
 * Years are discovered at runtime rather than hardcoded, so the suite keeps testing the real rule
 * as the calendar moves rather than a snapshot of it. Projection goes through
 * `occurrencesInHebrewYears`, which takes an explicit span and is therefore deterministic — unlike
 * `calculateYearlyOccurrences`, which is anchored to today by design.
 */
class HebrewCalendarEngineTest {

    private val searchRange = 5780..5850

    private fun firstYearWhere(predicate: (Int) -> Boolean): Int =
        searchRange.firstOrNull(predicate) ?: error("no qualifying Hebrew year in $searchRange")

    private fun project(
        originYear: Int,
        originMonth: Int,
        originDay: Int,
        targetYear: Int,
        rule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II
    ) = HebrewCalendarEngine.occurrencesInHebrewYears(
        originHebrewYear = originYear,
        originHebrewMonth = originMonth,
        originHebrewDay = originDay,
        leapYearRule = rule,
        years = targetYear..targetYear
    )

    // ---------- leap year handling ----------

    @Test
    fun `regular-year Adar defaults to Adar II in a leap year`() {
        val regularOrigin = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }
        val leapTarget = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }

        val occ = project(regularOrigin, JewishDate.ADAR, 10, leapTarget).single()

        assertEquals(JewishDate.ADAR_II, occ.targetHebrewMonth)
        assertEquals(10, occ.targetHebrewDay)
        assertTrue(occ.notes.contains(OccurrenceNote.LEAP_OBSERVED_IN_ADAR_II))
    }

    @Test
    fun `Adar I rule places the occurrence in Adar I`() {
        val regularOrigin = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }
        val leapTarget = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }

        val occ = project(regularOrigin, JewishDate.ADAR, 10, leapTarget, LeapYearRule.ADAR_I).single()

        assertEquals(JewishDate.ADAR, occ.targetHebrewMonth)
        assertTrue(occ.notes.contains(OccurrenceNote.LEAP_OBSERVED_IN_ADAR_I))
    }

    @Test
    fun `BOTH rule yields two occurrences in a leap year and one otherwise`() {
        val regularOrigin = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }
        val leapTarget = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }
        val regularTarget = firstYearWhere { it > leapTarget && !HebrewCalendarEngine.isLeapYear(it) }

        val inLeap = project(regularOrigin, JewishDate.ADAR, 10, leapTarget, LeapYearRule.BOTH)
        assertEquals(2, inLeap.size)
        assertEquals(listOf(JewishDate.ADAR, JewishDate.ADAR_II), inLeap.map { it.targetHebrewMonth })

        val inRegular = project(regularOrigin, JewishDate.ADAR, 10, regularTarget, LeapYearRule.BOTH)
        assertEquals(1, inRegular.size)
    }

    @Test
    fun `an Adar II origin collapses to Adar in a regular year`() {
        val leapOrigin = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }
        val regularTarget = firstYearWhere { it > leapOrigin && !HebrewCalendarEngine.isLeapYear(it) }

        val occ = project(leapOrigin, JewishDate.ADAR_II, 10, regularTarget).single()

        assertEquals(JewishDate.ADAR, occ.targetHebrewMonth)
        assertTrue(occ.notes.contains(OccurrenceNote.COLLAPSED_TO_SINGLE_ADAR))
    }

    @Test
    fun `an Adar I origin stays in Adar I in another leap year`() {
        val leapOrigin = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }
        val leapTarget = firstYearWhere { it > leapOrigin && HebrewCalendarEngine.isLeapYear(it) }

        val occ = project(leapOrigin, JewishDate.ADAR, 10, leapTarget).single()

        assertEquals(JewishDate.ADAR, occ.targetHebrewMonth)
        assertTrue(occ.notes.contains(OccurrenceNote.ORIGIN_ADAR_I_IN_LEAP_YEAR))
    }

    // ---------- short months ----------

    @Test
    fun `30 Cheshvan moves to 1 Kislev when Cheshvan is short`() {
        val shortCheshvan = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.CHESHVAN) == 29
        }
        val longCheshvan = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.CHESHVAN) == 30
        }

        val moved = project(longCheshvan, JewishDate.CHESHVAN, 30, shortCheshvan).single()
        assertEquals(JewishDate.KISLEV, moved.targetHebrewMonth)
        assertEquals(1, moved.targetHebrewDay)
        assertEquals(30, moved.adjustedFromDay)
        assertTrue(moved.notes.contains(OccurrenceNote.CHESHVAN_30_MOVED_TO_KISLEV_1))

        val unchanged = project(longCheshvan, JewishDate.CHESHVAN, 30, longCheshvan).single()
        assertEquals(JewishDate.CHESHVAN, unchanged.targetHebrewMonth)
        assertEquals(30, unchanged.targetHebrewDay)
    }

    @Test
    fun `30 Kislev moves to 1 Tevet when Kislev is short`() {
        val shortKislev = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.KISLEV) == 29
        }
        val longKislev = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.KISLEV) == 30
        }

        val moved = project(longKislev, JewishDate.KISLEV, 30, shortKislev).single()
        assertEquals(JewishDate.TEVES, moved.targetHebrewMonth)
        assertEquals(1, moved.targetHebrewDay)
        assertTrue(moved.notes.contains(OccurrenceNote.KISLEV_30_MOVED_TO_TEVET_1))
    }

    @Test
    fun `30 Adar I moves to 1 Nissan in a regular year`() {
        val leapOrigin = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }
        val regularTarget = firstYearWhere { it > leapOrigin && !HebrewCalendarEngine.isLeapYear(it) }

        // Adar I always has 30 days; Adar in a regular year has 29.
        assertEquals(30, HebrewCalendarEngine.getDaysInMonth(leapOrigin, JewishDate.ADAR))
        assertEquals(29, HebrewCalendarEngine.getDaysInMonth(regularTarget, JewishDate.ADAR))

        val occ = project(leapOrigin, JewishDate.ADAR, 30, regularTarget).single()
        assertEquals(JewishDate.NISSAN, occ.targetHebrewMonth)
        assertEquals(1, occ.targetHebrewDay)
        assertTrue(occ.notes.contains(OccurrenceNote.ADAR_30_MOVED_TO_NISSAN_1))
    }

    // ---------- invalid input is normalised, not thrown ----------

    @Test
    fun `Adar II normalises to Adar in a regular year`() {
        val regular = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }
        val leap = firstYearWhere { HebrewCalendarEngine.isLeapYear(it) }

        assertEquals(JewishDate.ADAR, HebrewCalendarEngine.normalizeMonth(regular, JewishDate.ADAR_II))
        assertEquals(JewishDate.ADAR_II, HebrewCalendarEngine.normalizeMonth(leap, JewishDate.ADAR_II))
    }

    @Test
    fun `fromHebrew tolerates an out-of-range day and an impossible month`() {
        val regular = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }

        // Would throw inside KosherJava if passed through unchecked.
        val info = HebrewCalendarEngine.fromHebrew(regular, JewishDate.ADAR_II, 45)

        assertEquals(JewishDate.ADAR, info.hebrewMonth)
        assertTrue(info.hebrewDay in 1..30)
    }

    // ---------- past occurrences ----------

    @Test
    fun `projection never starts in the past`() {
        val today = HebrewCalendarEngine.getToday()

        // An anniversary of today's Hebrew date: last year's is behind us, so the first result
        // must be today or later. This is what used to render as "Today!" all year round.
        val occurrences = HebrewCalendarEngine.calculateYearlyOccurrences(
            originHebrewYear = today.hebrewYear - 5,
            originHebrewMonth = today.hebrewMonth,
            originHebrewDay = today.hebrewDay,
            yearsCount = 3
        )

        assertFalse(occurrences.isEmpty())
        val todayKey = today.gregorianYear * 10_000 + today.gregorianMonth * 100 + today.gregorianDay
        val firstKey = occurrences.first().let {
            it.gregorianYear * 10_000 + it.gregorianMonth * 100 + it.gregorianDay
        }
        assertTrue("first occurrence $firstKey is before today $todayKey", firstKey >= todayKey)
    }

    @Test
    fun `yearly projection spans the requested number of Hebrew years`() {
        val today = HebrewCalendarEngine.getToday()
        val occurrences = HebrewCalendarEngine.calculateYearlyOccurrences(
            originHebrewYear = today.hebrewYear - 10,
            originHebrewMonth = JewishDate.TISHREI,
            originHebrewDay = 1,
            yearsCount = 7
        )
        assertEquals(7, occurrences.map { it.targetHebrewYear }.distinct().size)
    }

    // ---------- monthly recurrence ----------

    @Test
    fun `monthly recurrence advances through Adar II and into the next year`() {
        val occurrences = HebrewCalendarEngine.calculateMonthlyOccurrences(
            originHebrewDay = 15,
            monthsCount = 30
        )
        assertEquals(30, occurrences.size)

        // Strictly increasing, so the Adar II -> Nissan and Elul -> Tishrei transitions are sane.
        val keys = occurrences.map {
            it.gregorianYear * 10_000 + it.gregorianMonth * 100 + it.gregorianDay
        }
        assertEquals(keys.sorted(), keys)
        assertEquals(keys.distinct().size, keys.size)
    }

    @Test
    fun `monthly day is clamped in a short month`() {
        val shortCheshvan = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.CHESHVAN) == 29
        }
        assertEquals(29, HebrewCalendarEngine.monthlyDayIn(shortCheshvan, JewishDate.CHESHVAN, 30))
        assertEquals(12, HebrewCalendarEngine.monthlyDayIn(shortCheshvan, JewishDate.CHESHVAN, 12))
    }

    // ---------- conversion ----------

    @Test
    fun `Gregorian to Hebrew conversion round-trips`() {
        val info = HebrewCalendarEngine.fromGregorian(1993, 10, 13)
        assertEquals(JewishDate.TISHREI, info.hebrewMonth)
        assertEquals(28, info.hebrewDay)
        assertEquals(5754, info.hebrewYear)

        val back = HebrewCalendarEngine.fromHebrew(info.hebrewYear, info.hebrewMonth, info.hebrewDay)
        assertEquals(1993, back.gregorianYear)
        assertEquals(10, back.gregorianMonth)
        assertEquals(13, back.gregorianDay)
    }

    @Test
    fun `gregorianFormatted renders the civil date, not the Hebrew one`() {
        val info = HebrewCalendarEngine.fromGregorian(1993, 10, 13)
        assertEquals("13/10/1993", info.gregorianFormatted)
    }
}

/** The Hebrew day begins at nightfall, which is the difference between a yahrzeit being right or a day early. */
class SunsetConversionTest {

    @Test
    fun `after sunset advances the Hebrew date by one day`() {
        val before = HebrewCalendarEngine.fromGregorian(1993, 10, 13, afterSunset = false)
        val after = HebrewCalendarEngine.fromGregorian(1993, 10, 13, afterSunset = true)

        assertEquals(28, before.hebrewDay)
        assertEquals(29, after.hebrewDay)
        assertEquals(before.hebrewMonth, after.hebrewMonth)
    }

    @Test
    fun `after sunset keeps the civil date the user entered`() {
        val after = HebrewCalendarEngine.fromGregorian(1993, 10, 13, afterSunset = true)

        // The certificate still says the 13th; only the Hebrew reckoning moves.
        assertEquals(1993, after.gregorianYear)
        assertEquals(10, after.gregorianMonth)
        assertEquals(13, after.gregorianDay)
    }

    @Test
    fun `after sunset rolls over a Hebrew month end`() {
        // 29 Elul is the eve of Rosh Hashanah: after sunset it becomes 1 Tishrei of the next year.
        val eve = HebrewCalendarEngine.fromHebrew(5786, JewishDate.ELUL, 29)
        val after = HebrewCalendarEngine.fromGregorian(
            eve.gregorianYear, eve.gregorianMonth, eve.gregorianDay, afterSunset = true
        )

        assertEquals(JewishDate.TISHREI, after.hebrewMonth)
        assertEquals(1, after.hebrewDay)
        assertEquals(5787, after.hebrewYear)
    }
}

/** Reminder offsets are counted backwards from an all-day event's midnight start. */
class ReminderOptionTest {

    @Test
    fun `day before lands at 9am the previous day`() {
        // 1440 minutes back is the previous midnight; 900 is nine hours later than that.
        assertEquals(900, ReminderOption.DAY_BEFORE.minutes)
        assertEquals(1440 - 900, 540) // 09:00
    }

    @Test
    fun `week before is six further days back`() {
        assertEquals(900 + 6 * 24 * 60, ReminderOption.WEEK_BEFORE.minutes)
    }

    @Test
    fun `unknown or absent minutes resolve to none`() {
        assertEquals(ReminderOption.NONE, ReminderOption.fromMinutes(null))
        assertEquals(ReminderOption.NONE, ReminderOption.fromMinutes(12345))
        assertEquals(ReminderOption.DAY_BEFORE, ReminderOption.fromMinutes(900))
    }
}

/** A one-time event is the exact date entered — not an origin projected forward. */
class SingleOccurrenceTest {

    private val searchRange = 5780..5850

    private fun firstYearWhere(p: (Int) -> Boolean) =
        searchRange.first(p)

    @Test
    fun `a single occurrence lands on the exact date given`() {
        val occ = HebrewCalendarEngine.singleOccurrence(5787, JewishDate.KISLEV, 15)

        assertEquals(5787, occ.targetHebrewYear)
        assertEquals(JewishDate.KISLEV, occ.targetHebrewMonth)
        assertEquals(15, occ.targetHebrewDay)
        assertEquals(1, occ.occurrenceIndex)
    }

    @Test
    fun `it matches the conversion of that same Hebrew date`() {
        val occ = HebrewCalendarEngine.singleOccurrence(5787, JewishDate.KISLEV, 15)
        val info = HebrewCalendarEngine.fromHebrew(5787, JewishDate.KISLEV, 15)

        assertEquals(info.gregorianYear, occ.gregorianYear)
        assertEquals(info.gregorianMonth, occ.gregorianMonth)
        assertEquals(info.gregorianDay, occ.gregorianDay)
    }

    @Test
    fun `a past date is allowed rather than skipped`() {
        // Unlike the recurring projection, which deliberately drops what has already happened.
        val today = HebrewCalendarEngine.getToday()
        val occ = HebrewCalendarEngine.singleOccurrence(
            today.hebrewYear - 3, today.hebrewMonth, today.hebrewDay
        )
        assertEquals(today.hebrewYear - 3, occ.targetHebrewYear)
    }

    @Test
    fun `an impossible day still gets normalised`() {
        val shortCheshvan = firstYearWhere {
            HebrewCalendarEngine.getDaysInMonth(it, JewishDate.CHESHVAN) == 29
        }
        val occ = HebrewCalendarEngine.singleOccurrence(shortCheshvan, JewishDate.CHESHVAN, 30)

        assertEquals(JewishDate.KISLEV, occ.targetHebrewMonth)
        assertEquals(1, occ.targetHebrewDay)
        assertTrue(occ.notes.contains(OccurrenceNote.CHESHVAN_30_MOVED_TO_KISLEV_1))
    }

    @Test
    fun `Adar II in a regular year normalises to Adar`() {
        val regular = firstYearWhere { !HebrewCalendarEngine.isLeapYear(it) }
        val occ = HebrewCalendarEngine.singleOccurrence(regular, JewishDate.ADAR_II, 10)

        assertEquals(JewishDate.ADAR, occ.targetHebrewMonth)
    }
}

/** The weekday shown beside a converted date, which is how users sanity-check it. */
class WeekdayTest {

    @Test
    fun `weekday matches the civil date`() {
        // 13 October 1993 was a Wednesday.
        val info = HebrewCalendarEngine.fromGregorian(1993, 10, 13)
        assertEquals(java.util.Calendar.WEDNESDAY, info.dayOfWeek)
    }

    @Test
    fun `after sunset keeps the weekday of the date entered`() {
        // The Hebrew date moves forward, but the user typed the 13th and it was a Wednesday.
        val info = HebrewCalendarEngine.fromGregorian(1993, 10, 13, afterSunset = true)
        assertEquals(java.util.Calendar.WEDNESDAY, info.dayOfWeek)
        assertEquals(29, info.hebrewDay)
    }

    @Test
    fun `occurrences carry their own weekday`() {
        val occ = HebrewCalendarEngine.singleOccurrence(5787, JewishDate.KISLEV, 15)
        val info = HebrewCalendarEngine.fromHebrew(5787, JewishDate.KISLEV, 15)
        assertEquals(info.dayOfWeek, occ.dayOfWeek)
        assertTrue(occ.dayOfWeek in 1..7)
    }

    @Test
    fun `a Hebrew date never falls on a weekday it cannot`() {
        // Yom Kippur (10 Tishrei) can never be a Friday or Sunday — a classic sanity check
        // that the whole conversion chain is sound.
        for (year in 5780..5820) {
            val info = HebrewCalendarEngine.fromHebrew(year, JewishDate.TISHREI, 10)
            assertTrue(
                "10 Tishrei $year fell on ${info.dayOfWeek}",
                info.dayOfWeek != java.util.Calendar.FRIDAY &&
                    info.dayOfWeek != java.util.Calendar.SUNDAY
            )
        }
    }
}
