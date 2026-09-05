package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.db.HebrewEventEntity
import com.example.domain.model.EventType
import com.example.domain.model.LeapYearRule
import com.example.domain.model.RecurrenceType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun app_home_screenshot() {
        val strings = AppStrings(AppLanguage.HEBREW)
        val mockEvents = listOf(
            HebrewEventEntity(
                id = 1,
                title = "יום הולדת (עברי)",
                eventType = EventType.BIRTHDAY,
                recurrenceType = RecurrenceType.YEARLY,
                hebrewDay = 28,
                hebrewMonth = 7, // Tishrei
                hebrewYear = 5754,
                hebrewDateFormatted = "כ״ח בתשרי תשנ״ד",
                gregorianDay = 13,
                gregorianMonth = 10,
                gregorianYear = 1993,
                leapYearRule = LeapYearRule.STANDARD_ADAR_II,
                yearsCount = 20,
                occurrenceCount = 20,
                syncTag = "hcs-test",
                targetCalendarId = 1L,
                targetCalendarName = "Google Calendar",
                isSyncedToCalendar = true,
                syncedEventsCount = 20
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    strings = strings,
                    events = mockEvents,
                    onAddEventClick = {},
                    onDeleteEvent = {},
                    onExportIcs = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
