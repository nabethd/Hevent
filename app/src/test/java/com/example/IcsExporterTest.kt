package com.example

import com.example.domain.calendar.SyncLabels
import com.example.domain.ics.IcsEvent
import com.example.domain.ics.IcsExporter
import com.example.domain.model.CalculatedOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsExporterTest {

    private val labels = SyncLabels(hebrewDateLabel = "תאריך עברי", createdBy = "Hebrew Calendar Sync")

    private fun occurrence(day: Int) = CalculatedOccurrence(
        occurrenceIndex = 1,
        targetHebrewYear = 5786,
        targetHebrewMonth = 7,
        targetHebrewDay = 28,
        hebrewDateFormatted = "כ״ח בתשרי תשפ״ו",
        gregorianYear = 2025,
        gregorianMonth = 10,
        gregorianDay = day,
        gregorianDateFormatted = "%02d/10/2025".format(day),
        isLeapYear = false
    )

    private fun generate(title: String = "יום הולדת", seed: String = "hcs-abc") = IcsExporter.generateIcs(
        calendarName = "Test",
        events = listOf(IcsEvent(title, seed, listOf(occurrence(20)))),
        labels = labels
    )

    @Test
    fun `every content line stays within 75 octets`() {
        // Hebrew is two bytes per character in UTF-8, so unfolded description lines ran well past
        // the RFC 5545 limit and strict parsers truncated them.
        val lines = generate().split("\r\n").filter { it.isNotEmpty() }
        val tooLong = lines.filter { it.toByteArray(Charsets.UTF_8).size > 75 }
        assertTrue("lines over 75 octets: $tooLong", tooLong.isEmpty())
    }

    @Test
    fun `folded lines can be unfolded back to the original value`() {
        val unfolded = generate().replace("\r\n ", "")
        assertTrue(unfolded.contains("SUMMARY:יום הולדת"))
        assertTrue(unfolded.contains("BEGIN:VCALENDAR"))
        assertTrue(unfolded.contains("END:VCALENDAR"))
    }

    @Test
    fun `all-day DTEND is the following day`() {
        val unfolded = generate().replace("\r\n ", "")
        assertTrue(unfolded.contains("DTSTART;VALUE=DATE:20251020"))
        assertTrue(unfolded.contains("DTEND;VALUE=DATE:20251021"))
    }

    @Test
    fun `UID depends on the seed, not the title`() {
        // Renaming an event used to change every UID, so re-importing duplicated the whole series
        // instead of updating it.
        val uidOf: (String) -> String = { ics ->
            ics.replace("\r\n ", "").lines().first { it.startsWith("UID:") }
        }
        assertEquals(
            uidOf(generate(title = "Original")),
            uidOf(generate(title = "Renamed"))
        )
    }

    @Test
    fun `reserved characters are escaped`() {
        val ics = generate(title = "Birthday; party, at home\\shed").replace("\r\n ", "")
        val summary = ics.lines().first { it.startsWith("SUMMARY:") }
        assertEquals("SUMMARY:Birthday\\; party\\, at home\\\\shed", summary)
    }
}

class IcsAlarmTest {
    private val labels = SyncLabels(hebrewDateLabel = "Hebrew date", createdBy = "Hebrew Calendar Sync")

    private fun ics(reminder: Int?) = IcsExporter.generateIcs(
        calendarName = "Test",
        events = listOf(
            IcsEvent(
                title = "Birthday",
                uidSeed = "hcs-x",
                occurrences = listOf(
                    CalculatedOccurrence(
                        occurrenceIndex = 1, targetHebrewYear = 5786, targetHebrewMonth = 7,
                        targetHebrewDay = 28, hebrewDateFormatted = "x",
                        gregorianYear = 2025, gregorianMonth = 10, gregorianDay = 20,
                        gregorianDateFormatted = "20/10/2025", isLeapYear = false
                    )
                ),
                reminderMinutes = reminder
            )
        ),
        labels = labels
    ).replace("\r\n ", "")

    @Test
    fun `no VALARM when no reminder is set`() {
        assertTrue(!ics(null).contains("BEGIN:VALARM"))
    }

    @Test
    fun `VALARM triggers before the event`() {
        val out = ics(900)
        assertTrue(out.contains("BEGIN:VALARM"))
        assertTrue(out.contains("ACTION:DISPLAY"))
        assertTrue(out.contains("TRIGGER:-PT900M"))
        assertTrue(out.contains("END:VALARM"))
    }
}
