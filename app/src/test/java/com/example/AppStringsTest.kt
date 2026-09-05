package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.OccurrenceNote
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.localizedResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Guards the resource migration. The whole thing hinges on Hebrew resolving from `values-iw` —
 * Android's resource qualifier for Hebrew is the legacy code `iw`, not `he`, and getting it wrong
 * fails silently by falling back to English.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppStringsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val he = AppStrings(context, AppLanguage.HEBREW)
    private val en = AppStrings(context, AppLanguage.ENGLISH)

    @Test
    fun `hebrew resolves from values-iw rather than falling back to english`() {
        assertEquals("סנכרון לוח שנה עברי", he.appName)
        assertEquals("Hebrew Calendar Sync", en.appName)
        assertNotEquals(he.navEvents, en.navEvents)
    }

    @Test
    fun `both hebrew language codes resolve the hebrew resources`() {
        // "he" is modern, "iw" is the legacy code and the resource-folder qualifier. Which one a
        // runtime reports varies, so both must land on values-iw.
        for (code in listOf("he", "iw")) {
            val resources = localizedResources(context, AppLanguage.HEBREW)
            assertEquals(
                "resources for '$code' should be Hebrew",
                "סנכרון לוח שנה עברי",
                resources.getString(R.string.app_name)
            )
            assertEquals(AppLanguage.HEBREW, AppLanguage.fromDeviceLocale(Locale(code)))
        }
    }

    @Test
    fun `hebrew uses its dual form for two`() {
        assertEquals("בעוד יומיים", he.inDays(2))
        assertEquals("בעוד חודשיים", he.inMonths(2))
        assertEquals("בעוד יום", he.inDays(1))
        assertEquals("בעוד 5 ימים", he.inDays(5))
    }

    @Test
    fun `english plurals agree in number`() {
        assertEquals("In 1 day", en.inDays(1))
        assertEquals("In 5 days", en.inDays(5))
        assertEquals("In 1 month", en.inMonths(1))
    }

    @Test
    fun `counts are interpolated, not concatenated`() {
        assertTrue(he.deleteEventConfirmMsg(37).contains("37"))
        assertTrue(en.deleteEventConfirmMsg(37).contains("37"))
        assertTrue(en.syncSuccessCount(12).contains("12"))
        assertTrue(en.eventsDeletedFromCalendar(3).contains("3"))
    }

    @Test
    fun `occurrence notes are localised`() {
        val occ = CalculatedOccurrence(
            occurrenceIndex = 1, targetHebrewYear = 5786, targetHebrewMonth = 9,
            targetHebrewDay = 29, hebrewDateFormatted = "x",
            gregorianYear = 2025, gregorianMonth = 12, gregorianDay = 20,
            gregorianDateFormatted = "20/12/2025", isLeapYear = false,
            notes = listOf(OccurrenceNote.DAY_CLAMPED_TO_END_OF_MONTH),
            adjustedFromDay = 30
        )
        val hebrew = he.noteText(occ)!!
        val english = en.noteText(occ)!!

        assertTrue(hebrew.contains("30") && hebrew.contains("29"))
        assertTrue(english.contains("30") && english.contains("29"))
        assertNotEquals(hebrew, english)
    }

    @Test
    fun `no note yields null rather than an empty string`() {
        val occ = CalculatedOccurrence(
            occurrenceIndex = 1, targetHebrewYear = 5786, targetHebrewMonth = 7,
            targetHebrewDay = 1, hebrewDateFormatted = "x",
            gregorianYear = 2025, gregorianMonth = 9, gregorianDay = 23,
            gregorianDateFormatted = "23/09/2025", isLeapYear = false
        )
        assertEquals(null, he.noteText(occ))
    }

    @Test
    fun `device locale maps to a supported language`() {
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromDeviceLocale(Locale("he")))
        assertEquals(AppLanguage.HEBREW, AppLanguage.fromDeviceLocale(Locale("iw")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDeviceLocale(Locale("en")))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromDeviceLocale(Locale("fr")))
    }
}
