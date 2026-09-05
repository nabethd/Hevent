package com.example.ui.i18n

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
        "הוסיפו ימי הולדת, אזכרות, או ימי נישואין עבריים. האפליקציה תחשב את התאריכים ל-100 השנים הבאות ותסנכרן ישירות ליומן."
    else
        "Add Hebrew birthdays, yahrzeits, or anniversaries. The app calculates corresponding Gregorian dates for 100 years and syncs directly to your calendar."
    val addEventFab: String = if (isHe) "הוסף אירוע חדש" else "Add New Event"
    val eventsCountLabel: String = if (isHe) "אירועים מתוזמנים" else "Scheduled Events"
    val nextUpcoming: String = if (isHe) "מועדים קרובים" else "Upcoming Occurrences"
    val syncedBadge: String = if (isHe) "מסונכרן ליומן" else "Synced to Calendar"
    val icsBadge: String = if (isHe) "קובץ ICS" else "ICS File"
    val deleteEventConfirmTitle: String = if (isHe) "מחיקת אירוע" else "Delete Event"
    val deleteEventConfirmMsg: String = if (isHe)
        "האם למחוק אירוע זה וכל 100 המופעים שלו מהיומן וממאגר האפליקציה?"
    else
        "Delete this event and all 100 occurrences from your calendar and app storage?"
    val delete: String = if (isHe) "מחק" else "Delete"
    val cancel: String = if (isHe) "ביטול" else "Cancel"
    val exportIcs: String = if (isHe) "ייצא קובץ ICS" else "Export ICS"
    val syncToCalendar: String = if (isHe) "סנכרן ליומן" else "Sync to Calendar"

    // Add Event Dialog / Form
    val addEventTitle: String = if (isHe) "הוספת אירוע עברי חוזר" else "Add Recurring Hebrew Event"
    val eventNameLabel: String = if (isHe) "שם האירוע (לדוגמה: יום הולדת דרור עברי)" else "Event Name (e.g. Dror's Birthday - Hebrew)"
    val eventNamePlaceholder: String = if (isHe) "יום הולדת / אזכרה / יום נישואין" else "Birthday / Yahrzeit / Anniversary"
    val eventTypeLabel: String = if (isHe) "סוג אירוע" else "Event Type"
    val typeBirthday: String = if (isHe) "יום הולדת" else "Birthday"
    val typeYahrzeit: String = if (isHe) "אזכרה (יארצייט)" else "Yahrzeit"
    val typeAnniversary: String = if (isHe) "יום נישואין" else "Anniversary"
    val typeCustom: String = if (isHe) "אירוע כללי" else "Custom Event"

    // Date Mode Selection
    val dateInputMode: String = if (isHe) "אופן הזנת התאריך המקורי" else "Date Input Method"
    val modeGregorian: String = if (isHe) "תאריך לועזי (המרה אוטומטית)" else "Gregorian Date (Auto-convert)"
    val modeHebrew: String = if (isHe) "תאריך עברי ישיר" else "Direct Hebrew Date"

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
        "מחיקה בטוחה של כל מופעי האירוע מהיומן לפי שמו, תוך שמירה על אירועים אישיים אחרים"
    else
        "Safely remove all occurrences of an event by title from the calendar"
    val enterNameToDelete: String = if (isHe) "הזן את שם האירוע למחיקה" else "Enter exact event title to delete"
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
    val inDaysPrefix: String = if (isHe) "בעוד" else "In"
    val daysSuffix: String = if (isHe) "ימים" else "days"
    val leapYearTag: String = if (isHe) "שנה מעוברת (13 חודשים)" else "Leap Year (13 mo.)"
    val regularYearTag: String = if (isHe) "שנה פשוטה (12 חודשים)" else "Regular Year (12 mo.)"
}
