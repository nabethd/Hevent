package com.example.ui.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.example.R
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.OccurrenceNote
import java.util.Locale

/**
 * A [Resources] bound to [language] rather than to the device locale.
 *
 * The app has its own language switch that takes effect without a restart, so string lookup
 * cannot simply follow the system configuration. This builds a configuration context for the
 * chosen language and reads resources through it.
 */
fun localizedResources(context: Context, language: AppLanguage): Resources {
    // Android aliases the Hebrew codes, so a "he" locale still resolves values-iw.
    val locale = Locale(language.code)
    val config = Configuration(context.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return context.createConfigurationContext(config).resources
}

/**
 * Typed access to the UI strings.
 *
 * The text itself lives in `res/values/strings.xml` and `res/values-iw/strings.xml`; this is a
 * thin facade over it. Keeping the facade (rather than calling `stringResource` at every call
 * site) is deliberate: the ViewModel also needs strings — for snackbars and for the text embedded
 * in calendar entries — and it has no composition to read them from.
 */
class AppStrings(val lang: AppLanguage, private val res: Resources) {

    constructor(context: Context, language: AppLanguage) :
        this(language, localizedResources(context, language))

    val isHe: Boolean = lang == AppLanguage.HEBREW

    val aboutSection: String = res.getString(R.string.about_section)
    val aboutText: String = res.getString(R.string.about_text)
    val addAnotherEventBtn: String = res.getString(R.string.add_another_event_btn)
    val addAnotherToBatch: String = res.getString(R.string.add_another_to_batch)
    val addEventFab: String = res.getString(R.string.add_event_fab)
    val addEventForDay: String = res.getString(R.string.add_event_for_day)
    val addEventTitle: String = res.getString(R.string.add_event_title)
    val afterSunsetHint: String = res.getString(R.string.after_sunset_hint)
    val afterSunsetLabel: String = res.getString(R.string.after_sunset_label)
    val appName: String = res.getString(R.string.app_name)
    val calendarCreateErrorFallback: String = res.getString(R.string.calendar_create_error_fallback)
    val calendarCreateFailedMsg: String = res.getString(R.string.calendar_create_failed_msg)
    val calendarCreatedSuccess: String = res.getString(R.string.calendar_created_success)
    val calendarManagementSection: String = res.getString(R.string.calendar_management_section)
    val calendarManagementSubtitle: String = res.getString(R.string.calendar_management_subtitle)
    val calendarViewTitle: String = res.getString(R.string.calendar_view_title)
    val cancel: String = res.getString(R.string.cancel)
    val clearAll: String = res.getString(R.string.clear_all)
    val connectedCalendarsTitle: String = res.getString(R.string.connected_calendars_title)
    val convertedEquivalent: String = res.getString(R.string.converted_equivalent)
    val createCalendarBtn: String = res.getString(R.string.create_calendar_btn)
    val createCalendarSubtitle: String = res.getString(R.string.create_calendar_subtitle)
    val createCalendarTitle: String = res.getString(R.string.create_calendar_title)
    val createdBy: String = res.getString(R.string.created_by)
    val dateInputMode: String = res.getString(R.string.date_input_mode)
    val dayLabel: String = res.getString(R.string.day_label)
    val dayHasEvents: String = res.getString(R.string.day_has_events)
    val defaultCalendarName: String = res.getString(R.string.default_calendar_name)
    val delete: String = res.getString(R.string.delete)
    val deleteAllByNameSubtitle: String = res.getString(R.string.delete_all_by_name_subtitle)
    val deleteAllByNameTitle: String = res.getString(R.string.delete_all_by_name_title)
    val googleCalendarCreateFailed: String = res.getString(R.string.google_calendar_create_failed)
    val googleSyncFailed: String = res.getString(R.string.google_sync_failed)
    val deleteCloudLeftover: String = res.getString(R.string.delete_cloud_leftover)
    fun googleCalendarReady(name: String): String = res.getString(R.string.google_calendar_ready, name)
    val deleteEventConfirmTitle: String = res.getString(R.string.delete_event_confirm_title)
    val deleteSuccess: String = res.getString(R.string.delete_success)
    val destDeviceCalendar: String = res.getString(R.string.dest_device_calendar)
    val destIcsOnly: String = res.getString(R.string.dest_ics_only)
    val destNewCalendar: String = res.getString(R.string.dest_new_calendar)
    val destinationLabel: String = res.getString(R.string.destination_label)
    val durationYears: String = res.getString(R.string.duration_years)
    val edit: String = res.getString(R.string.edit)
    val editEventTitle: String = res.getString(R.string.edit_event_title)
    val emptyEventsSubtitle: String = res.getString(R.string.empty_events_subtitle)
    val emptyEventsTitle: String = res.getString(R.string.empty_events_title)
    val enterNameToDelete: String = res.getString(R.string.enter_name_to_delete)
    val eventNameLabel: String = res.getString(R.string.event_name_label)
    val eventNamePlaceholder: String = res.getString(R.string.event_name_placeholder)
    val eventSaved: String = res.getString(R.string.event_saved)
    val eventTypeLabel: String = res.getString(R.string.event_type_label)
    val eventUpdated: String = res.getString(R.string.event_updated)
    val eventsCountLabel: String = res.getString(R.string.events_count_label)
    val eventsDeletedFromCal: String = res.getString(R.string.events_deleted_from_cal)
    val eventsOnDay: String = res.getString(R.string.events_on_day)
    val executeDeleteBtn: String = res.getString(R.string.execute_delete_btn)
    val exportAllIcsSubtitle: String = res.getString(R.string.export_all_ics_subtitle)
    val exportAllIcsTitle: String = res.getString(R.string.export_all_ics_title)
    val exportFailed: String = res.getString(R.string.export_failed)
    val exportIcs: String = res.getString(R.string.export_ics)
    val fillTitleError: String = res.getString(R.string.fill_title_error)
    val fri: String = res.getString(R.string.fri)
    val genericError: String = res.getString(R.string.generic_error)
    val googleCalendarNotice: String = res.getString(R.string.google_calendar_notice)
    val grantPermissionBtn: String = res.getString(R.string.grant_permission_btn)
    val gregorianEquivalent: String = res.getString(R.string.gregorian_equivalent)
    val hebrewDateLabel: String = res.getString(R.string.hebrew_date_label)
    val icsBadge: String = res.getString(R.string.ics_badge)
    val icsExportReady: String = res.getString(R.string.ics_export_ready)
    val languageSection: String = res.getString(R.string.language_section)
    val languageSubtitle: String = res.getString(R.string.language_subtitle)
    val leapYear13Months: String = res.getString(R.string.leap_year_13_months)
    val leapYearHalachicNote: String = res.getString(R.string.leap_year_halachic_note)
    val leapYearRuleTitle: String = res.getString(R.string.leap_year_rule_title)
    val leapYearTag: String = res.getString(R.string.leap_year_tag)
    val loadingCalendars: String = res.getString(R.string.loading_calendars)
    val localCalendarHint: String = res.getString(R.string.local_calendar_hint)
    val modeGregorian: String = res.getString(R.string.mode_gregorian)
    val modeHebrew: String = res.getString(R.string.mode_hebrew)
    val mon: String = res.getString(R.string.mon)
    val monthLabel: String = res.getString(R.string.month_label)
    val navCalendar: String = res.getString(R.string.nav_calendar)
    val navEvents: String = res.getString(R.string.nav_events)
    val navSettings: String = res.getString(R.string.nav_settings)
    val newCalendarNameLabel: String = res.getString(R.string.new_calendar_name_label)
    val newCalendarNote: String = res.getString(R.string.new_calendar_note)
    val newEventShort: String = res.getString(R.string.new_event_short)
    val nextMonth: String = res.getString(R.string.next_month)
    val nextUpcoming: String = res.getString(R.string.next_upcoming)
    val noActiveCalendars: String = res.getString(R.string.no_active_calendars)
    val noEventsOnDay: String = res.getString(R.string.no_events_on_day)
    val noEventsToDelete: String = res.getString(R.string.no_events_to_delete)
    val permissionRequiredMsg: String = res.getString(R.string.permission_required_msg)
    val pickFromList: String = res.getString(R.string.pick_from_list)
    val prevMonth: String = res.getString(R.string.prev_month)
    val previewOccurrencesTitle: String = res.getString(R.string.preview_occurrences_title)
    val recommendedForGoogle: String = res.getString(R.string.recommended_for_google)
    val recurMonthly: String = res.getString(R.string.recur_monthly)
    val recurMonthlyShort: String = res.getString(R.string.recur_monthly_short)
    val recurYearly: String = res.getString(R.string.recur_yearly)
    val recurYearlyShort: String = res.getString(R.string.recur_yearly_short)
    val recurrenceLabel: String = res.getString(R.string.recurrence_label)
    val regularYearTag: String = res.getString(R.string.regular_year_tag)
    val reminderDayBefore: String = res.getString(R.string.reminder_day_before)
    val reminderLabel: String = res.getString(R.string.reminder_label)
    val reminderNone: String = res.getString(R.string.reminder_none)
    val reminderWeekBefore: String = res.getString(R.string.reminder_week_before)
    val ruleAdarI: String = res.getString(R.string.rule_adar_i)
    val ruleBoth: String = res.getString(R.string.rule_both)
    val ruleStandardAdarII: String = res.getString(R.string.rule_standard_adar_i_i)
    val sat: String = res.getString(R.string.sat)
    val saveAndExportIcsBtn: String = res.getString(R.string.save_and_export_ics_btn)
    val saveAndSyncBtn: String = res.getString(R.string.save_and_sync_btn)
    val saveChanges: String = res.getString(R.string.save_changes)
    val savingInProgress: String = res.getString(R.string.saving_in_progress)
    val selectCalendar: String = res.getString(R.string.select_calendar)
    val settingsTitle: String = res.getString(R.string.settings_title)
    val stagedEventsCount: String = res.getString(R.string.staged_events_count)
    val sun: String = res.getString(R.string.sun)
    val syncPartial: String = res.getString(R.string.sync_partial)
    val syncSuccess: String = res.getString(R.string.sync_success)
    val syncToCalendar: String = res.getString(R.string.sync_to_calendar)
    val syncedBadge: String = res.getString(R.string.synced_badge)
    val thu: String = res.getString(R.string.thu)
    val todayBadge: String = res.getString(R.string.today_badge)
    val todayBtn: String = res.getString(R.string.today_btn)
    val todayHebrewDate: String = res.getString(R.string.today_hebrew_date)
    val tomorrowBadge: String = res.getString(R.string.tomorrow_badge)
    val trackedEvents: String = res.getString(R.string.tracked_events)
    val tue: String = res.getString(R.string.tue)
    val typeAnniversary: String = res.getString(R.string.type_anniversary)
    val typeBirthday: String = res.getString(R.string.type_birthday)
    val destGoogleCloudNew: String = res.getString(R.string.dest_google_cloud_new)
    val destGoogleCloudNewDesc: String = res.getString(R.string.dest_google_cloud_new_desc)
    val googleCloudStepNotice: String = res.getString(R.string.google_cloud_step_notice)
    val googleConnectBtn: String = res.getString(R.string.google_connect_btn)
    val googleConnectedAs: String = res.getString(R.string.google_connected_as)
    val googleChangeAccount: String = res.getString(R.string.google_change_account)
    val googleCloudCalendarNameLabel: String = res.getString(R.string.google_cloud_calendar_name_label)
    val googleCloudDefaultName: String = res.getString(R.string.google_cloud_default_name)
    val googleSyncSuccess: String = res.getString(R.string.google_sync_success)
    val googleAuthFailed: String = res.getString(R.string.google_auth_failed)
    val googleCloudSection: String = res.getString(R.string.google_cloud_section)
    val googleCloudSectionDesc: String = res.getString(R.string.google_cloud_section_desc)
    val recommendedActionBadge: String = res.getString(R.string.recommended_action_badge)
    val btnCreateAndSync: String = res.getString(R.string.btn_create_and_sync)
    fun btnCreateAndSyncCount(count: Int): String = res.getString(R.string.btn_create_and_sync_count, count)
    fun googleSyncSuccessCount(count: Int): String = res.getString(R.string.google_sync_success_count, count)

    val typeCustom: String = res.getString(R.string.type_custom)
    val typeHoliday: String = res.getString(R.string.type_holiday)
    val typeYahrzeit: String = res.getString(R.string.type_yahrzeit)
    val wed: String = res.getString(R.string.wed)
    val yearLabel: String = res.getString(R.string.year_label)
    val yearsAbbrev: String = res.getString(R.string.years_abbrev)
    val yearsAhead: String = res.getString(R.string.years_ahead)
    val yearsCountLabel: String = res.getString(R.string.years_count_label)
    val yearsNotice: String = res.getString(R.string.years_notice)
    val yearsSuffix: String = res.getString(R.string.years_suffix)
    val yearsUnit: String = res.getString(R.string.years_unit)

    fun deleteEventConfirmMsg(occurrences: Int): String =
        res.getString(R.string.delete_event_confirm_msg, occurrences)

    fun exportEventsCount(count: Int): String = res.getString(R.string.export_events_count, count)

    fun syncEventsCount(count: Int): String = res.getString(R.string.sync_events_count, count)

    fun queuedForSync(count: Int): String = res.getString(R.string.queued_for_sync, count)

    fun syncSuccessCount(count: Int): String = res.getString(R.string.sync_success_count, count)

    /** Uses real plurals, so Hebrew gets its dual form ("יומיים", not "2 ימים"). */
    fun inDays(days: Int): String = res.getQuantityString(R.plurals.in_days, days, days)

    fun inMonths(months: Int): String = res.getQuantityString(R.plurals.in_months, months, months)

    fun eventsDeletedFromCalendar(count: Int): String =
        res.getQuantityString(R.plurals.events_deleted_from_calendar, count, count)

    /** Human text for a calculated occurrence's structured notes. */
    fun noteText(occ: CalculatedOccurrence): String? = occ.notes
        .map { note ->
            when (note) {
            OccurrenceNote.LEAP_OBSERVED_IN_ADAR_II -> res.getString(R.string.note_leap_observed_in_adar_ii)
            OccurrenceNote.LEAP_OBSERVED_IN_ADAR_I -> res.getString(R.string.note_leap_observed_in_adar_i)
            OccurrenceNote.LEAP_BOTH_ADAR_I -> res.getString(R.string.note_leap_both_adar_i)
            OccurrenceNote.LEAP_BOTH_ADAR_II -> res.getString(R.string.note_leap_both_adar_ii)
            OccurrenceNote.ORIGIN_ADAR_I_IN_LEAP_YEAR -> res.getString(R.string.note_origin_adar_i_in_leap_year)
            OccurrenceNote.ORIGIN_ADAR_II_IN_LEAP_YEAR -> res.getString(R.string.note_origin_adar_ii_in_leap_year)
            OccurrenceNote.COLLAPSED_TO_SINGLE_ADAR -> res.getString(R.string.note_collapsed_to_single_adar)
            OccurrenceNote.CHESHVAN_30_MOVED_TO_KISLEV_1 -> res.getString(R.string.note_cheshvan_30_moved_to_kislev_1)
            OccurrenceNote.KISLEV_30_MOVED_TO_TEVET_1 -> res.getString(R.string.note_kislev_30_moved_to_tevet_1)
            OccurrenceNote.ADAR_30_MOVED_TO_NISSAN_1 -> res.getString(R.string.note_adar_30_moved_to_nissan_1)
                OccurrenceNote.DAY_CLAMPED_TO_END_OF_MONTH -> res.getString(
                    R.string.note_day_clamped_to_end_of_month,
                    occ.adjustedFromDay ?: occ.targetHebrewDay,
                    occ.targetHebrewDay
                )
            }
        }
        .joinToString(" \u2022 ")
        .ifBlank { null }
}
