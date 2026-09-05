package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.HebrewEventEntity
import com.example.domain.calendar.DeviceCalendarInfo
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.model.EventType
import com.example.domain.model.LeapYearRule
import com.example.domain.model.RecurrenceType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AddEditEventDialog
import com.example.ui.screens.CalendarViewScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/** Renders each screen to PNG so layout and colour can be reviewed without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenshotTest {

    @get:Rule val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val he = AppStrings(context, AppLanguage.HEBREW)
    private val en = AppStrings(context, AppLanguage.ENGLISH)

    private val today = HebrewCalendarEngine.getToday()

    private val sampleEvents = listOf(
        HebrewEventEntity(
            id = 1, title = "יום הולדת אמא", eventType = EventType.BIRTHDAY,
            recurrenceType = RecurrenceType.YEARLY,
            hebrewDay = 28, hebrewMonth = 7, hebrewYear = 5754,
            hebrewDateFormatted = "כ״ח בתשרי תשנ״ד",
            gregorianDay = 13, gregorianMonth = 10, gregorianYear = 1993,
            leapYearRule = LeapYearRule.STANDARD_ADAR_II,
            yearsCount = 20, occurrenceCount = 20, syncTag = "hcs-1",
            targetCalendarId = 1L, targetCalendarName = "Google Calendar",
            isSyncedToCalendar = true, syncedEventsCount = 20
        ),
        HebrewEventEntity(
            id = 2, title = "אזכרה סבא", eventType = EventType.YAHRZEIT,
            recurrenceType = RecurrenceType.YEARLY,
            hebrewDay = 12, hebrewMonth = 12, hebrewYear = 5760,
            hebrewDateFormatted = "י״ב באדר תש״ס",
            gregorianDay = 19, gregorianMonth = 3, gregorianYear = 2000,
            leapYearRule = LeapYearRule.BOTH,
            yearsCount = 50, occurrenceCount = 68, syncTag = "hcs-2",
            targetCalendarId = 1L, targetCalendarName = "Google Calendar",
            isSyncedToCalendar = true, syncedEventsCount = 68
        ),
        HebrewEventEntity(
            id = 3, title = "יום נישואין", eventType = EventType.ANNIVERSARY,
            recurrenceType = RecurrenceType.YEARLY,
            hebrewDay = 3, hebrewMonth = 3, hebrewYear = 5780,
            hebrewDateFormatted = "ג׳ בסיוון תש״פ",
            gregorianDay = 26, gregorianMonth = 5, gregorianYear = 2020,
            leapYearRule = LeapYearRule.STANDARD_ADAR_II,
            yearsCount = 20, occurrenceCount = 20, syncTag = null,
            isSyncedToCalendar = false, syncedEventsCount = 0
        )
    )

    private val calendars = listOf(
        DeviceCalendarInfo(1, "drorna@gmail.com", "drorna@gmail.com", "com.google", 0xFF1A56DB.toInt(), true, false),
        DeviceCalendarInfo(2, "אירועים עבריים", "local", "LOCAL", 0xFF059669.toInt(), false, true)
    )

    private fun shot(name: String, dark: Boolean, rtl: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            MyApplicationTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Box(Modifier.fillMaxSize().let { it }) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) { content() }
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$name.png")
    }

    @Test fun home_light_he() = shot("home_light_he", dark = false, rtl = true) {
        HomeScreen(he, sampleEvents, {}, {}, {}, {})
    }

    @Test fun home_dark_he() = shot("home_dark_he", dark = true, rtl = true) {
        HomeScreen(he, sampleEvents, {}, {}, {}, {})
    }

    @Test fun home_light_en_empty() = shot("home_light_en_empty", dark = false, rtl = false) {
        HomeScreen(en, emptyList(), {}, {}, {}, {})
    }

    @Test fun calendar_light_he() = shot("calendar_light_he", dark = false, rtl = true) {
        val now = Calendar.getInstance()
        CalendarViewScreen(
            strings = he,
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) + 1,
            selectedDay = now.get(Calendar.DAY_OF_MONTH),
            events = sampleEvents,
            onPrevMonth = {}, onNextMonth = {}, onTodayClick = {},
            onDaySelect = {}, onAddEventForDate = {}
        )
    }

    @Test fun calendar_dark_he() = shot("calendar_dark_he", dark = true, rtl = true) {
        val now = Calendar.getInstance()
        CalendarViewScreen(
            strings = he,
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) + 1,
            selectedDay = now.get(Calendar.DAY_OF_MONTH),
            events = sampleEvents,
            onPrevMonth = {}, onNextMonth = {}, onTodayClick = {},
            onDaySelect = {}, onAddEventForDate = {}
        )
    }

    /** October 2026 contains the 28 Tishrei birthday, so the day dot must render. */
    @Test fun calendar_light_he_with_events() = shot("calendar_events_he", dark = false, rtl = true) {
        CalendarViewScreen(
            strings = he, year = 2026, month = 10, selectedDay = 9,
            events = sampleEvents,
            onPrevMonth = {}, onNextMonth = {}, onTodayClick = {},
            onDaySelect = {}, onAddEventForDate = {}
        )
    }

    @Test fun dialog_edit_he() = shot("dialog_edit_he", dark = false, rtl = true) {
        AddEditEventDialog(
            strings = he, availableCalendars = calendars, hasCalendarPermission = true,
            isSyncing = false, stagedEvents = emptyList(), prefillDate = null,
            editingEvent = sampleEvents[1],
            onRequestCalendarPermission = {}, onCreateNewCalendar = { null },
            onStageEvent = {}, onUnstageEvent = {}, onClearStaged = {},
            onDismiss = {}, onSave = { _, _, _, _ -> }
        )
    }

    @Test fun settings_light_he() = shot("settings_light_he", dark = false, rtl = true) {
        SettingsScreen(
            strings = he, currentLanguage = AppLanguage.HEBREW, onLanguageChange = {},
            events = sampleEvents, availableCalendars = calendars, hasCalendarPermission = true,
            onRequestCalendarPermission = {}, onCreateNewCalendar = { null },
            onDeleteByName = {}, onExportAllIcs = {}
        )
    }

    @Test fun dialog_light_he() = shot("dialog_light_he", dark = false, rtl = true) {
        AddEditEventDialog(
            strings = he, availableCalendars = calendars, hasCalendarPermission = true,
            isSyncing = false, stagedEvents = emptyList(), prefillDate = today,
            onRequestCalendarPermission = {}, onCreateNewCalendar = { null },
            onStageEvent = {}, onUnstageEvent = {}, onClearStaged = {},
            onDismiss = {}, onSave = { _, _, _, _ -> }
        )
    }

    @Test fun dialog_dark_he() = shot("dialog_dark_he", dark = true, rtl = true) {
        AddEditEventDialog(
            strings = he, availableCalendars = calendars, hasCalendarPermission = true,
            isSyncing = false, stagedEvents = emptyList(), prefillDate = today,
            onRequestCalendarPermission = {}, onCreateNewCalendar = { null },
            onStageEvent = {}, onUnstageEvent = {}, onClearStaged = {},
            onDismiss = {}, onSave = { _, _, _, _ -> }
        )
    }
}
