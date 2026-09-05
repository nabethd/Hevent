package com.example.ui.i18n

import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.OccurrenceNote

class AppStrings(val lang: AppLanguage) {
    val isHe = lang == AppLanguage.HEBREW

    // App header & Navigation
    val appName: String = if (isHe) "סנכרון לוח שנה עברי" else "Hebrew Calendar Sync"
    val navEvents: String = if (isHe) "אירועים" else "Events"
    val navCalendar: String = if (isHe) "לוח שנה" else "Calendar"
    val navSettings: String = if (isHe) "הגדרות" else "Settings"

    // Home / Events List
    val emptyEventsTitle: String = if (isHe) "אין עדיין אירועים עבריים" else "No Hebrew Events Yet"
    val emptyEventsSubtitle: String = if (isHe)
        "הוסיפו ימי הולדת, אזכרות, או ימי נישואין עבריים. האפליקציה תחשב את התאריכים הלועזיים המקבילים ותסנכרן אותם ישירות ליומן."
    else
        "Add Hebrew birthdays, yahrzeits, or anniversaries. The app calculates the matching Gregorian dates and syncs them straight to your calendar."
    val addEventFab: String = if (isHe) "הוסף אירוע חדש" else "Add New Event"
    val eventsCountLabel: String = if (isHe) "אירועים מתוזמנים" else "Scheduled Events"
    val nextUpcoming: String = if (isHe) "מועדים קרובים" else "Upcoming Occurrences"
    val syncedBadge: String = if (isHe) "מסונכרן ליומן" else "Synced to Calendar"
    val icsBadge: String = if (isHe) "קובץ ICS" else "ICS File"
    val deleteEventConfirmTitle: String = if (isHe) "מחיקת אירוע" else "Delete Event"
    fun deleteEventConfirmMsg(occurrences: Int): String = if (isHe)
        "האם למחוק אירוע זה ואת כל $occurrences המופעים שלו מהיומן וממאגר האפליקציה?"
    else
        "Delete this event and all $occurrences occurrences from your calendar and app storage?"
    val delete: String = if (isHe) "מחק" else "Delete"
    val cancel: String = if (isHe) "ביטול" else "Cancel"
    val exportIcs: String = if (isHe) "ייצא קובץ ICS" else "Export ICS"
    val syncToCalendar: String = if (isHe) "סנכרן ליומן" else "Sync to Calendar"

    // Add Event Dialog / Form
    val addEventTitle: String = if (isHe) "הוספת אירוע עברי חוזר" else "Add Recurring Hebrew Event"
    val eventNameLabel: String = if (isHe) "שם האירוע" else "Event Name"
    val eventNamePlaceholder: String = if (isHe) "יום הולדת / אזכרה / יום נישואין" else "Birthday / Yahrzeit / Anniversary"
    val eventTypeLabel: String = if (isHe) "סוג אירוע" else "Event Type"
    val typeBirthday: String = if (isHe) "יום הולדת" else "Birthday"
    val typeYahrzeit: String = if (isHe) "אזכרה (יארצייט)" else "Yahrzeit"
    val typeAnniversary: String = if (isHe) "יום נישואין" else "Anniversary"
    val typeCustom: String = if (isHe) "אירוע כללי" else "Custom Event"

    // Date Mode Selection
    val dateInputMode: String = if (isHe) "אופן הזנת התאריך המקורי" else "Date Input Method"
    val modeGregorian: String = if (isHe) "תאריך לועזי" else "Gregorian"
    val modeHebrew: String = if (isHe) "תאריך עברי" else "Hebrew"

    val dayLabel: String = if (isHe) "יום" else "Day"
    val monthLabel: String = if (isHe) "חודש" else "Month"
    val yearLabel: String = if (isHe) "שנה" else "Year"

    val convertedEquivalent: String = if (isHe) "התאריך העברי המקביל:" else "Equivalent Hebrew Date:"
    val gregorianEquivalent: String = if (isHe) "התאריך הלועזי המקביל:" else "Equivalent Gregorian Date:"

    // Recurrence
    val recurrenceLabel: String = if (isHe) "מחזוריות חזרת האירוע" else "Recurrence Cycle"
    val recurYearly: String = if (isHe) "שנתי" else "Yearly"
    val recurMonthly: String = if (isHe) "חודשי (כל ראש חודש / יום קבוע בחודש)" else "Monthly (Recurring Hebrew day of month)"
    val yearsCountLabel: String = if (isHe) "משך תזמון החזרות:" else "Repeat duration:"
    val yearsSuffix: String = if (isHe) "שנים" else "years"
    val yearsNotice: String = if (isHe) "האירוע יחושב ויסונכרן לשנים אלו לפי כללי העיבור והלוח העברי" else "Calculated and synced for these years according to Jewish calendar rules"
    val recurYearlyShort: String = if (isHe) "שנתי" else "Yearly"
    val recurMonthlyShort: String = if (isHe) "חודשי" else "Monthly"
    val previewOccurrencesTitle: String = if (isHe) "תצוגה מקדימה של המועדים הקרובים" else "Preview of Upcoming Occurrences"

    // Leap Year Rules (Halacha)
    val leapYearRuleTitle: String = if (isHe) "כלל הלכתי לשנה מעוברת (אדר א׳ / אדר ב׳)" else "Halachic Rule for Leap Years (Adar I / II)"
    val ruleStandardAdarII: String = if (isHe)
        "ברירת מחדל הלכתית: אדר ב׳ (בר מצווה וימי הולדת)"
    else
        "Standard Halacha: Adar II (Birthdays & Bar Mitzvah)"
    val ruleAdarI: String = if (isHe)
        "אדר א׳"
    else
        "Adar I"
    val ruleBoth: String = if (isHe)
        "שני האדרים (אדר א׳ וגם אדר ב׳ - נהוג באזכרות)"
    else
        "Both Adars (Adar I & II - common for Yahrzeits)"
    val leapYearHalachicNote: String = if (isHe)
        "על פי ההלכה הפסוקה (שו״ע או״ח נה, ט; תקסח, ז): יום הולדת שנולד בשנה פשוטה נחגג בשנה מעוברת באדר ב׳. עבור אזכרה יש הנוהגים באדר א׳ או בשני האדרים."
    else
        "According to standard Halacha (Shulchan Aruch OC 55:9; 568:7): Birthdays from a regular year are observed in Adar II during leap years. For Yahrzeits, customs vary between Adar I, Adar II, or both."

    // Destination options
    val destinationLabel: String = if (isHe) "יעד הסנכרון" else "Sync Destination"
    val destDeviceCalendar: String = if (isHe) "סנכרן ישירות ליומן Google / המכשיר" else "Sync directly to Google / Device Calendar"
    val destNewCalendar: String = if (isHe) "צור יומן ייעודי חדש (״אירועים עבריים״)" else "Create a new dedicated calendar (\"Hebrew Events\")"
    val destIcsOnly: String = if (isHe) "הורד / ייצא קובץ ICS בלבד" else "Export / Share ICS file only"

    val selectCalendar: String = if (isHe) "בחר יומן יעד" else "Select Target Calendar"
    val permissionRequiredMsg: String = if (isHe)
        "נדרשת הרשאת גישה ליומן כדי להוסיף את המופעים ישירות ליומן Google / המכשיר."
    else
        "Calendar permission is required to add occurrences directly into Google / device calendar."
    val grantPermissionBtn: String = if (isHe) "מתן הרשאות יומן Google" else "Grant Calendar Permission"

    // Google Calendar connection note
    val googleCalendarNotice: String = if (isHe)
        "סנכרון ליומן Google מתבצע באופן מאובטח באמצעות חשבון Google המחובר למכשירך. אין צורך בהתחברות נוספת בדפדפן."
    else
        "Syncing to Google Calendar operates securely through your device's active Google account. No browser login needed."
    val calendarCreateErrorFallback: String = if (isHe)
        "יצירת יומן נפרד אינה נתמכת במכשיר זה עקב מדיניות האבטחה של אנדרואיד. בחרנו עבורך סנכרון ישיר ליומן Google הקיים."
    else
        "Creating a standalone calendar is restricted by Android security on this device. Switched to your active Google/Device calendar."
    val calendarCreateFailedMsg: String = if (isHe)
        "יצירת יומן מקומי נכשלה (מגבלת מערכת אנדרואיד במכשיר זה). מומלץ לסנכרן ליומן Google הקיים או לייצא קובץ ICS."
    else
        "Creating local calendar failed (Android device policy). Please sync to existing Google Calendar or export as ICS."

    // Multi-event batching in Dialog
    val addAnotherEventBtn: String = if (isHe) "+ הוסף אירוע נוסף לסבב" else "+ Add another event to batch"
    val stagedEventsCount: String = if (isHe) "אירועים מוכנים לסנכרון:" else "Events queued for sync:"
    val saveAndSyncBtn: String = if (isHe) "שמור וסנכרן" else "Save & Sync"
    val saveAndExportIcsBtn: String = if (isHe) "שמור וייצא קובץ ICS" else "Save & Export ICS File"

    // Calendar Screen
    val calendarViewTitle: String = if (isHe) "לוח שנה עברי - לועזי" else "Hebrew - Gregorian Calendar"
    val todayBtn: String = if (isHe) "היום" else "Today"
    val sun: String = if (isHe) "א׳" else "Sun"
    val mon: String = if (isHe) "ב׳" else "Mon"
    val tue: String = if (isHe) "ג׳" else "Tue"
    val wed: String = if (isHe) "ד׳" else "Wed"
    val thu: String = if (isHe) "ה׳" else "Thu"
    val fri: String = if (isHe) "ו׳" else "Fri"
    val sat: String = if (isHe) "ש׳" else "Sat"
    val noEventsOnDay: String = if (isHe) "אין אירועים מסונכרנים ביום זה" else "No scheduled events on this day"
    val eventsOnDay: String = if (isHe) "אירועים ביום זה:" else "Events on this day:"

    // Settings Screen
    val settingsTitle: String = if (isHe) "הגדרות" else "Settings"
    val languageSection: String = if (isHe) "שפת הממשק" else "App Language"
    val languageSubtitle: String = if (isHe) "מעבר מיידי בין עברית לאנגלית ללא צורך בהפעלה מחדש" else "Instant toggle between Hebrew and English without app restart"
    val calendarManagementSection: String = if (isHe) "יומנים וחיבור" else "Calendars & Connection"
    val calendarManagementSubtitle: String = if (isHe)
        "ניהול יומני המכשיר, יצירת יומן עברי ייעודי, ומחיקת אירועים מרוכזת"
    else
        "Manage device calendars, create dedicated Hebrew calendars, and bulk delete events"
    val createCalendarTitle: String = if (isHe) "יצירת יומן ייעודי חדש" else "Create Dedicated Calendar"
    val createCalendarSubtitle: String = if (isHe)
        "יצירת לוח שנה מקומי ייעודי (״אירועים עבריים״) לריכוז כל התאריכים בנפרד"
    else
        "Create a dedicated local calendar (\"Hebrew Events\") to separate Hebrew dates"
    val createCalendarBtn: String = if (isHe) "צור יומן עברי חדש" else "Create Hebrew Calendar"
    val calendarCreatedSuccess: String = if (isHe) "היומן נוצר בהצלחה!" else "Calendar created successfully!"
    val connectedCalendarsTitle: String = if (isHe) "יומנים זמינים במכשיר:" else "Available Device Calendars:"
    val deleteAllByNameTitle: String = if (isHe) "מחיקת אירועים לפי שם" else "Delete All Events by Name"
    val deleteAllByNameSubtitle: String = if (isHe)
        "מוחק רק מופעים שאפליקציה זו יצרה. אירועים שיצרתם בעצמכם לעולם לא ייגעו."
    else
        "Removes only occurrences this app created. Events you made yourself are never touched."
    val enterNameToDelete: String = if (isHe) "בחר אירוע למחיקה" else "Choose an event to delete"
    val executeDeleteBtn: String = if (isHe) "מחק אירועים אלו מהיומן" else "Delete These Events from Calendar"
    val exportAllIcsTitle: String = if (isHe) "ייצוא כל האירועים לקובץ ICS" else "Export All Events to ICS"
    val exportAllIcsSubtitle: String = if (isHe) "שמירה וגיבוי של כל המועדים לקובץ שניתן לייבא לכל יומן" else "Save and backup all dates into a universal file for any calendar"
    val aboutSection: String = if (isHe) "אודות חישוב התאריכים העבריים" else "About Hebrew Date Calculations"
    val aboutText: String = if (isHe)
        "האפליקציה מחשבת את המקבילה הגרגוריאנית של כל תאריך עברי עבור כל שנה עתידית בהתאם לכללי העיבור, אדר א׳ ואדר ב׳, וחודשים חסרים ומלאים (חשוון וכסלו). כל אירוע מוזרק כאירוע 'יום שלם' עצמאי, לדיוק מושלם ללא תלות בתמיכה מקורית של יומנים."
    else
        "The app calculates the Gregorian equivalent for each Hebrew date for future years following halachic intercalation rules, Adar I vs Adar II, and defective/full months (Cheshvan/Kislev). Each occurrence is injected as a standalone all-day event for guaranteed multi-decade precision."

    // Toasts & Notifications
    val syncSuccess: String = if (isHe) "אירועים סונכרנו בהצלחה ליומן!" else "Events successfully synced to calendar!"
    val eventSaved: String = if (isHe) "האירוע נשמר בהצלחה" else "Event saved successfully"
    val icsExportReady: String = if (isHe) "קובץ ICS מוכן לשיתוף וייבוא" else "ICS file is ready for sharing and import"
    val deleteSuccess: String = if (isHe) "האירוע נמחק בהצלחה" else "Event deleted successfully"
    val eventsDeletedFromCal: String = if (isHe) "אירועים נמחקו מיומן המכשיר" else "events deleted from device calendar"
    val fillTitleError: String = if (isHe) "נא להזין שם אירוע" else "Please enter an event name"
    val todayHebrewDate: String = if (isHe) "היום בלוח העברי" else "Today in Hebrew Calendar"
    val todayBadge: String = if (isHe) "היום!" else "Today!"
    val tomorrowBadge: String = if (isHe) "מחר" else "Tomorrow"
    fun inDays(days: Int): String = if (isHe) {
        when (days) {
            2 -> "בעוד יומיים"
            else -> "בעוד $days ימים"
        }
    } else {
        "In $days days"
    }

    fun inMonths(months: Int): String = if (isHe) {
        when (months) {
            1 -> "בעוד חודש"
            2 -> "בעוד חודשיים"
            else -> "בעוד $months חודשים"
        }
    } else {
        if (months == 1) "In 1 month" else "In $months months"
    }
    val leapYearTag: String = if (isHe) "שנה מעוברת (13 חודשים)" else "Leap Year (13 mo.)"
    val regularYearTag: String = if (isHe) "שנה פשוטה (12 חודשים)" else "Regular Year (12 mo.)"

    // Labels embedded in the calendar rows / ICS files we generate. These used to be hardcoded
    // Hebrew, so English users got Hebrew descriptions inside their own calendar.
    val hebrewDateLabel: String = if (isHe) "תאריך עברי" else "Hebrew date"
    val createdBy: String = if (isHe) "נוצר באמצעות Hebrew Calendar Sync" else "Created with Hebrew Calendar Sync"

    val syncPartial: String = if (isHe)
        "האירוע נשמר, אך סנכרון היומן לא הושלם"
    else
        "Event saved, but calendar sync did not complete"
    val genericError: String = if (isHe)
        "משהו השתבש. האירוע לא נשמר."
    else
        "Something went wrong. The event was not saved."
    val eventUpdated: String = if (isHe) "האירוע עודכן" else "Event updated"
    val editEventTitle: String = if (isHe) "עריכת אירוע" else "Edit Event"
    val saveChanges: String = if (isHe) "שמור שינויים" else "Save Changes"
    val edit: String = if (isHe) "ערוך" else "Edit"

    // Sunset: the Hebrew day begins at nightfall, so a birth or death after it belongs to the
    // next Hebrew day. Without this the yahrzeit is a day early.
    val afterSunsetLabel: String = if (isHe) "אחרי השקיעה" else "After sunset"
    val afterSunsetHint: String = if (isHe)
        "היום העברי מתחיל בצאת הכוכבים. אם האירוע היה אחרי השקיעה, התאריך העברי הוא של היום שלמחרת."
    else
        "The Hebrew day begins at nightfall. If the event was after sunset, the Hebrew date is the following day."

    val reminderLabel: String = if (isHe) "תזכורת" else "Reminder"
    val reminderNone: String = if (isHe) "ללא" else "None"
    val reminderDayBefore: String = if (isHe) "יום לפני, 09:00" else "Day before, 09:00"
    val reminderWeekBefore: String = if (isHe) "שבוע לפני, 09:00" else "Week before, 09:00"

    val exportFailed: String = if (isHe) "ייצוא הקובץ נכשל" else "Export failed"
    val savingInProgress: String = if (isHe) "שומר..." else "Saving..."
    val noEventsToDelete: String = if (isHe) "לא נמצאו אירועים בשם זה" else "No events found with that name"

    /** Human text for a calculated occurrence's structured notes. */
    fun noteText(occ: CalculatedOccurrence): String? {
        val parts = occ.notes.map { note ->
            when (note) {
                OccurrenceNote.LEAP_OBSERVED_IN_ADAR_II ->
                    if (isHe) "שנה מעוברת: נחגג באדר ב׳" else "Leap year: observed in Adar II"
                OccurrenceNote.LEAP_OBSERVED_IN_ADAR_I ->
                    if (isHe) "שנה מעוברת: נחגג באדר א׳" else "Leap year: observed in Adar I"
                OccurrenceNote.LEAP_BOTH_ADAR_I ->
                    if (isHe) "שני האדרים: אדר א׳" else "Both Adars: Adar I"
                OccurrenceNote.LEAP_BOTH_ADAR_II ->
                    if (isHe) "שני האדרים: אדר ב׳" else "Both Adars: Adar II"
                OccurrenceNote.ORIGIN_ADAR_I_IN_LEAP_YEAR ->
                    if (isHe) "שנה מעוברת: אדר א׳" else "Leap year: Adar I"
                OccurrenceNote.ORIGIN_ADAR_II_IN_LEAP_YEAR ->
                    if (isHe) "שנה מעוברת: אדר ב׳" else "Leap year: Adar II"
                OccurrenceNote.COLLAPSED_TO_SINGLE_ADAR ->
                    if (isHe) "שנה פשוטה: אדר" else "Regular year: Adar"
                OccurrenceNote.CHESHVAN_30_MOVED_TO_KISLEV_1 ->
                    if (isHe) "ל׳ בחשוון חסר ← א׳ בכסלו" else "30 Cheshvan absent - moved to 1 Kislev"
                OccurrenceNote.KISLEV_30_MOVED_TO_TEVET_1 ->
                    if (isHe) "ל׳ בכסלו חסר ← א׳ בטבת" else "30 Kislev absent - moved to 1 Tevet"
                OccurrenceNote.ADAR_30_MOVED_TO_NISSAN_1 ->
                    if (isHe) "ל׳ באדר חסר ← א׳ בניסן" else "30 Adar absent - moved to 1 Nissan"
                OccurrenceNote.DAY_CLAMPED_TO_END_OF_MONTH -> {
                    val from = occ.adjustedFromDay
                    if (isHe) "יום $from אינו קיים ← הותאם ליום ${occ.targetHebrewDay}"
                    else "Day $from does not exist - adjusted to day ${occ.targetHebrewDay}"
                }
            }
        }
        return parts.joinToString(" \u2022 ").ifBlank { null }
    }
}
