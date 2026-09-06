package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.calendar.GoogleCalendarCloudManager
import com.example.domain.calendar.SyncLabels
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.EventColor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Covers the pure parts of the cloud client: the request body, and which failures are worth
 * retrying. The network calls themselves are not exercised here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleCalendarCloudManagerTest {

    private val manager =
        GoogleCalendarCloudManager(ApplicationProvider.getApplicationContext<Context>())

    private val labels = SyncLabels(hebrewDateLabel = "תאריך עברי", createdBy = "Hebrew Calendar Sync")

    private val occurrence = CalculatedOccurrence(
        occurrenceIndex = 1,
        targetHebrewYear = 5787, targetHebrewMonth = 7, targetHebrewDay = 28,
        hebrewDateFormatted = "כ״ח בתשרי תשפ״ז",
        gregorianYear = 2026, gregorianMonth = 10, gregorianDay = 9,
        gregorianDateFormatted = "09/10/2026", isLeapYear = false
    )

    private fun body(reminder: Int? = null) = JSONObject(
        manager.eventJson("יום הולדת אמא", occurrence, "hcs-abc123", labels, reminder, null, null)
    )

    // ---------- the handle that makes deletion possible ----------

    @Test
    fun `the sync tag is written to private extended properties`() {
        // Without this, cloud events cannot be found again and deleting the app event would
        // leave its occurrences in the user's Google Calendar permanently.
        val priv = body().getJSONObject("extendedProperties").getJSONObject("private")
        assertEquals("hcs-abc123", priv.getString(GoogleCalendarCloudManager.PROP_SYNC_ID))
    }

    // ---------- all-day dates ----------

    @Test
    fun `all-day event uses date, and end is the following day`() {
        val json = body()
        assertEquals("2026-10-09", json.getJSONObject("start").getString("date"))
        assertEquals("2026-10-10", json.getJSONObject("end").getString("date"))
        assertFalse(json.getJSONObject("start").has("dateTime"))
    }

    @Test
    fun `end date rolls over a month boundary`() {
        val endOfMonth = occurrence.copy(gregorianYear = 2026, gregorianMonth = 10, gregorianDay = 31)
        val json = JSONObject(
            manager.eventJson("x", endOfMonth, "hcs-1", labels, null, null, null)
        )
        assertEquals("2026-10-31", json.getJSONObject("start").getString("date"))
        assertEquals("2026-11-01", json.getJSONObject("end").getString("date"))
    }

    @Test
    fun `dates stay ASCII under a locale with its own digits`() {
        val original = Locale.getDefault()
        try {
            // An Arabic-Indic locale would otherwise render "٢٠٢٦-١٠-٠٩", which the API rejects.
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            val date = body().getJSONObject("start").getString("date")
            assertEquals("2026-10-09", date)
            assertTrue(date.all { it.isDigit() && it.code < 128 || it == '-' })
        } finally {
            Locale.setDefault(original)
        }
    }

    // ---------- reminders ----------

    @Test
    fun `reminder is emitted only when requested`() {
        assertFalse(body(reminder = null).has("reminders"))

        val reminders = body(reminder = 900).getJSONObject("reminders")
        assertFalse(reminders.getBoolean("useDefault"))
        val override = reminders.getJSONArray("overrides").getJSONObject(0)
        assertEquals(900, override.getInt("minutes"))
        assertEquals("popup", override.getString("method"))
    }

    // ---------- description ----------

    @Test
    fun `description is built from the supplied labels, not hardcoded text`() {
        val english = SyncLabels(hebrewDateLabel = "Hebrew date", createdBy = "Made by the app")
        val json = JSONObject(
            manager.eventJson("Birthday", occurrence, "hcs-1", english, null, null, "Leap year: Adar II")
        )
        val description = json.getString("description")
        assertTrue(description.startsWith("Hebrew date: "))
        assertTrue(description.contains("Leap year: Adar II"))
        assertTrue(description.contains("Made by the app"))
    }

    // ---------- colour ----------

    @Test
    fun `colorId is sent only when a colour was chosen`() {
        // The API takes an index into its own palette, not an RGB value, so the default must be
        // omitted entirely rather than sent as some approximation of the calendar's colour.
        assertFalse(body().has("colorId"))

        val coloured = JSONObject(
            manager.eventJson(
                "Birthday", occurrence, "hcs-1", labels, null,
                EventColor.TOMATO.googleColorId, null
            )
        )
        assertEquals("11", coloured.getString("colorId"))
    }

    @Test
    fun `the default colour has no google id`() {
        assertEquals(null, EventColor.DEFAULT.googleColorId)
        EventColor.entries.filter { it != EventColor.DEFAULT }.forEach {
            assertTrue("${it.name} needs a colorId", it.googleColorId != null)
        }
    }

    // ---------- retry classification ----------

    @Test
    fun `transient failures are retried`() {
        assertTrue(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(429, "")))
        assertTrue(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(500, "")))
        assertTrue(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(503, "")))
        // Calendar signals quota exhaustion as a 403 with a reason, unlike a real denial.
        assertTrue(
            manager.isRetryable(
                GoogleCalendarCloudManager.HttpResult(403, """{"error":{"errors":[{"reason":"rateLimitExceeded"}]}}""")
            )
        )
    }

    @Test
    fun `permanent failures are not retried`() {
        assertFalse(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(200, "")))
        assertFalse(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(400, "bad request")))
        assertFalse(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(401, "unauthorized")))
        assertFalse(manager.isRetryable(GoogleCalendarCloudManager.HttpResult(404, "not found")))
        // A genuine permission denial must surface, not spin.
        assertFalse(
            manager.isRetryable(
                GoogleCalendarCloudManager.HttpResult(403, """{"error":{"errors":[{"reason":"forbidden"}]}}""")
            )
        )
    }
}
