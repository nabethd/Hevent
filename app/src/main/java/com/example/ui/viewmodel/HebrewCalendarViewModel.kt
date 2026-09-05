package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.HebrewEventEntity
import com.example.data.repository.HebrewEventRepository
import com.example.domain.calendar.CalendarSyncManager
import com.example.domain.calendar.DeviceCalendarInfo
import com.example.domain.calendar.SyncLabels
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.ics.IcsEvent
import com.example.domain.ics.IcsExporter
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.EventType
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.LeapYearRule
import com.example.domain.model.RecurrenceType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** One-shot effects. A StateFlow would swallow two identical messages in a row. */
sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Share(val intent: Intent, val chooserTitle: String) : UiEvent
    data object Saved : UiEvent
}

class HebrewCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "HebrewCalendarVM"
        const val PREFS = "hebrew_calendar_prefs"
        const val KEY_LANGUAGE = "language"
    }

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val repository: HebrewEventRepository =
        HebrewEventRepository(AppDatabase.getInstance(application).hebrewEventDao())

    val calendarSyncManager: CalendarSyncManager = CalendarSyncManager(application)

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _events.receiveAsFlow()

    // Language — persisted, so the choice survives a restart.
    private val _language = MutableStateFlow(
        AppLanguage.entries.firstOrNull { it.code == prefs.getString(KEY_LANGUAGE, null) }
            ?: AppLanguage.HEBREW
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val strings: AppStrings get() = AppStrings(_language.value)

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
        prefs.edit().putString(KEY_LANGUAGE, newLanguage.code).apply()
    }

    val events: StateFlow<List<HebrewEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _calendars = MutableStateFlow<List<DeviceCalendarInfo>>(emptyList())
    val calendars: StateFlow<List<DeviceCalendarInfo>> = _calendars.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(calendarSyncManager.hasCalendarPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Events queued in the add dialog. Held here rather than in composition so a rotation does not
     * discard a batch the user spent minutes assembling.
     */
    private val _stagedEvents = MutableStateFlow<List<EventDraft>>(emptyList())
    val stagedEvents: StateFlow<List<EventDraft>> = _stagedEvents.asStateFlow()

    // Calendar view navigation
    private val _calViewYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calViewYear: StateFlow<Int> = _calViewYear.asStateFlow()

    private val _calViewMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val calViewMonth: StateFlow<Int> = _calViewMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    /** Set when the user taps "add event for this day" so the dialog can open on that date. */
    private val _prefillDate = MutableStateFlow<HebrewDateInfo?>(null)
    val prefillDate: StateFlow<HebrewDateInfo?> = _prefillDate.asStateFlow()

    init {
        refreshCalendarPermissions()
    }

    fun refreshCalendarPermissions() {
        val granted = calendarSyncManager.hasCalendarPermission()
        _hasCalendarPermission.value = granted
        if (granted) loadDeviceCalendars()
    }

    fun loadDeviceCalendars() {
        viewModelScope.launch { _calendars.value = calendarSyncManager.getAvailableCalendars() }
    }

    fun setCalendarMonth(year: Int, month: Int) {
        _calViewYear.value = year
        _calViewMonth.value = month
        clampSelectedDay()
    }

    fun prevCalendarMonth() {
        val m = _calViewMonth.value - 1
        if (m < 1) { _calViewMonth.value = 12; _calViewYear.value-- } else _calViewMonth.value = m
        clampSelectedDay()
    }

    fun nextCalendarMonth() {
        val m = _calViewMonth.value + 1
        if (m > 12) { _calViewMonth.value = 1; _calViewYear.value++ } else _calViewMonth.value = m
        clampSelectedDay()
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day.coerceIn(1, daysInViewMonth())
    }

    /**
     * Keeps the selection inside the visible month. Without this, moving from the 31st to a short
     * month left `selectedDay` out of range and Calendar's lenient mode silently rolled the date
     * into the following month.
     */
    private fun clampSelectedDay() {
        _selectedDay.value = _selectedDay.value.coerceIn(1, daysInViewMonth())
    }

    private fun daysInViewMonth(): Int = Calendar.getInstance().apply {
        clear()
        set(_calViewYear.value, _calViewMonth.value - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun requestAddEventForDate(date: HebrewDateInfo?) { _prefillDate.value = date }
    fun consumePrefillDate() { _prefillDate.value = null }

    suspend fun createNewHebrewCalendar(name: String): Long? {
        val id = calendarSyncManager.createNewHebrewCalendar(name)
        if (id != null) loadDeviceCalendars()
        return id
    }

    data class EventDraft(
        val title: String,
        val eventType: EventType,
        val recurrenceType: RecurrenceType,
        val hebrewDateInfo: HebrewDateInfo,
        val leapYearRule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II,
        val yearsCount: Int = 20,
        val afterSunset: Boolean = false,
        val reminderMinutes: Int? = null
    )

    fun stageEvent(draft: EventDraft) { _stagedEvents.value = _stagedEvents.value + draft }
    fun unstageEvent(index: Int) {
        _stagedEvents.value = _stagedEvents.value.filterIndexed { i, _ -> i != index }
    }
    fun clearStagedEvents() { _stagedEvents.value = emptyList() }

    private fun occurrencesFor(
        recurrence: RecurrenceType,
        hebrewDay: Int,
        hebrewMonth: Int,
        hebrewYear: Int,
        rule: LeapYearRule,
        years: Int
    ): List<CalculatedOccurrence> = when (recurrence) {
        RecurrenceType.MONTHLY -> HebrewCalendarEngine.calculateMonthlyOccurrences(
            originHebrewDay = hebrewDay,
            monthsCount = years * 12
        )
        RecurrenceType.YEARLY -> HebrewCalendarEngine.calculateYearlyOccurrences(
            originHebrewYear = hebrewYear,
            originHebrewMonth = hebrewMonth,
            originHebrewDay = hebrewDay,
            leapYearRule = rule,
            yearsCount = years
        )
    }

    private fun occurrencesFor(event: HebrewEventEntity): List<CalculatedOccurrence> = occurrencesFor(
        event.recurrenceType,
        event.hebrewDay,
        event.hebrewMonth,
        event.hebrewYear,
        event.leapYearRule,
        event.yearsCount
    )

    private fun syncLabels() = SyncLabels(
        hebrewDateLabel = strings.hebrewDateLabel,
        createdBy = strings.createdBy
    )

    /**
     * Saves the staged drafts plus [extra], syncing to a calendar or producing an ICS file.
     * Guarded by [isSyncing]: the save button is disabled while this runs, which is what stops a
     * double tap from writing two full sets of calendar rows.
     */
    fun saveEvents(
        extra: EventDraft?,
        targetCalendarId: Long?,
        targetCalendarName: String?,
        isIcsOnly: Boolean
    ) {
        if (_isSyncing.value) return
        val drafts = _stagedEvents.value + listOfNotNull(extra)
        if (drafts.isEmpty()) return

        viewModelScope.launch {
            _isSyncing.value = true
            val localStrings = strings
            try {
                var totalSynced = 0
                val icsEvents = mutableListOf<IcsEvent>()

                for (draft in drafts) {
                    val occurrences = occurrencesFor(
                        draft.recurrenceType,
                        draft.hebrewDateInfo.hebrewDay,
                        draft.hebrewDateInfo.hebrewMonth,
                        draft.hebrewDateInfo.hebrewYear,
                        draft.leapYearRule,
                        draft.yearsCount.coerceAtLeast(1)
                    )
                    val syncTag = CalendarSyncManager.newSyncTag()

                    var syncedCount = 0
                    if (!isIcsOnly && targetCalendarId != null && calendarSyncManager.hasCalendarPermission()) {
                        syncedCount = calendarSyncManager.insertEvents(
                            calendarId = targetCalendarId,
                            eventTitle = draft.title,
                            occurrences = occurrences,
                            syncTag = syncTag,
                            labels = syncLabels(),
                            reminderMinutes = draft.reminderMinutes,
                            noteFor = { localStrings.noteText(it) }
                        )
                        totalSynced += syncedCount
                    }

                    repository.insertEvent(
                        HebrewEventEntity(
                            title = draft.title,
                            eventType = draft.eventType,
                            recurrenceType = draft.recurrenceType,
                            hebrewDay = draft.hebrewDateInfo.hebrewDay,
                            hebrewMonth = draft.hebrewDateInfo.hebrewMonth,
                            hebrewYear = draft.hebrewDateInfo.hebrewYear,
                            hebrewDateFormatted = draft.hebrewDateInfo.formattedHe,
                            gregorianDay = draft.hebrewDateInfo.gregorianDay,
                            gregorianMonth = draft.hebrewDateInfo.gregorianMonth,
                            gregorianYear = draft.hebrewDateInfo.gregorianYear,
                            leapYearRule = draft.leapYearRule,
                            // yearsCount is what the user asked for; occurrenceCount is what it produced.
                            yearsCount = draft.yearsCount,
                            occurrenceCount = occurrences.size,
                            afterSunset = draft.afterSunset,
                            reminderMinutes = draft.reminderMinutes,
                            syncTag = syncTag,
                            targetCalendarId = targetCalendarId.takeUnless { isIcsOnly },
                            targetCalendarName = targetCalendarName.takeUnless { isIcsOnly },
                            isSyncedToCalendar = syncedCount > 0,
                            syncedEventsCount = syncedCount
                        )
                    )
                    icsEvents += IcsEvent(draft.title, syncTag, occurrences, draft.reminderMinutes)
                }

                clearStagedEvents()

                if (isIcsOnly) {
                    val name = if (drafts.size == 1) drafts[0].title else "hebrew_events"
                    val intent = buildShareIntent(
                        calendarName = name,
                        events = icsEvents,
                        fileName = name,
                        localStrings = localStrings
                    )
                    _events.send(UiEvent.Share(intent, localStrings.exportIcs))
                } else {
                    _events.send(
                        UiEvent.Message(
                            when {
                                totalSynced > 0 -> "${localStrings.syncSuccess} ($totalSynced)"
                                targetCalendarId != null -> localStrings.syncPartial
                                else -> localStrings.eventSaved
                            }
                        )
                    )
                }
                _events.send(UiEvent.Saved)
            } catch (e: Exception) {
                // Never surface a raw exception message to the user.
                Log.e(TAG, "Saving events failed", e)
                _events.send(UiEvent.Message(localStrings.genericError))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Replaces an existing event. The old calendar rows are removed by their own sync tag and a
     * fresh set is written, because the dates themselves may have moved.
     */
    fun updateEvent(
        original: HebrewEventEntity,
        draft: EventDraft,
        targetCalendarId: Long?,
        targetCalendarName: String?,
        isIcsOnly: Boolean
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val localStrings = strings
            try {
                if (calendarSyncManager.hasCalendarPermission()) {
                    calendarSyncManager.deleteSyncedEvents(
                        syncTag = original.syncTag,
                        title = original.title,
                        calendarId = original.targetCalendarId
                    )
                }

                val occurrences = occurrencesFor(
                    draft.recurrenceType,
                    draft.hebrewDateInfo.hebrewDay,
                    draft.hebrewDateInfo.hebrewMonth,
                    draft.hebrewDateInfo.hebrewYear,
                    draft.leapYearRule,
                    draft.yearsCount.coerceAtLeast(1)
                )
                val syncTag = CalendarSyncManager.newSyncTag()

                var syncedCount = 0
                if (!isIcsOnly && targetCalendarId != null && calendarSyncManager.hasCalendarPermission()) {
                    syncedCount = calendarSyncManager.insertEvents(
                        calendarId = targetCalendarId,
                        eventTitle = draft.title,
                        occurrences = occurrences,
                        syncTag = syncTag,
                        labels = syncLabels(),
                        reminderMinutes = draft.reminderMinutes,
                        noteFor = { localStrings.noteText(it) }
                    )
                }

                repository.updateEvent(
                    original.copy(
                        title = draft.title,
                        eventType = draft.eventType,
                        recurrenceType = draft.recurrenceType,
                        hebrewDay = draft.hebrewDateInfo.hebrewDay,
                        hebrewMonth = draft.hebrewDateInfo.hebrewMonth,
                        hebrewYear = draft.hebrewDateInfo.hebrewYear,
                        hebrewDateFormatted = draft.hebrewDateInfo.formattedHe,
                        gregorianDay = draft.hebrewDateInfo.gregorianDay,
                        gregorianMonth = draft.hebrewDateInfo.gregorianMonth,
                        gregorianYear = draft.hebrewDateInfo.gregorianYear,
                        leapYearRule = draft.leapYearRule,
                        yearsCount = draft.yearsCount,
                        occurrenceCount = occurrences.size,
                        afterSunset = draft.afterSunset,
                        reminderMinutes = draft.reminderMinutes,
                        syncTag = syncTag,
                        targetCalendarId = targetCalendarId.takeUnless { isIcsOnly },
                        targetCalendarName = targetCalendarName.takeUnless { isIcsOnly },
                        isSyncedToCalendar = syncedCount > 0,
                        syncedEventsCount = syncedCount
                    )
                )
                _events.send(UiEvent.Message(localStrings.eventUpdated))
                _events.send(UiEvent.Saved)
            } catch (e: Exception) {
                Log.e(TAG, "Updating event failed", e)
                _events.send(UiEvent.Message(localStrings.genericError))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** Deletes one event and only the calendar rows it created. */
    fun deleteEvent(event: HebrewEventEntity) {
        viewModelScope.launch {
            val localStrings = strings
            if (calendarSyncManager.hasCalendarPermission()) {
                calendarSyncManager.deleteSyncedEvents(
                    syncTag = event.syncTag,
                    title = event.title,
                    calendarId = event.targetCalendarId
                )
            }
            repository.deleteEvent(event)
            _events.send(UiEvent.Message(localStrings.deleteSuccess))
        }
    }

    /**
     * Bulk delete by title. Resolves the title to rows we own first, then deletes each row's own
     * calendar events by its sync tag — the title never reaches the calendar provider as a bare
     * match, which is what previously let this wipe identically-named events the user created.
     */
    fun deleteEventsByName(name: String) {
        viewModelScope.launch {
            val localStrings = strings
            val matches = repository.getEventsByTitle(name)
            if (matches.isEmpty()) {
                _events.send(UiEvent.Message(localStrings.noEventsToDelete))
                return@launch
            }
            var deletedFromCalendar = 0
            if (calendarSyncManager.hasCalendarPermission()) {
                for (event in matches) {
                    deletedFromCalendar += calendarSyncManager.deleteSyncedEvents(
                        syncTag = event.syncTag,
                        title = event.title,
                        calendarId = event.targetCalendarId
                    )
                }
            }
            repository.deleteEventsByIds(matches.map { it.id })
            _events.send(
                UiEvent.Message("$deletedFromCalendar ${localStrings.eventsDeletedFromCal}")
            )
        }
    }

    /** ICS export. Projection and file IO both happen off the main thread. */
    fun exportEventsToIcs(eventsToExport: List<HebrewEventEntity>, singleTitle: String? = null) {
        if (eventsToExport.isEmpty()) return
        viewModelScope.launch {
            val localStrings = strings
            try {
                val name = singleTitle ?: "all_hebrew_events"
                val intent = withContext(Dispatchers.Default) {
                    buildShareIntent(
                        calendarName = name,
                        events = eventsToExport.map {
                            IcsEvent(
                                title = it.title,
                                uidSeed = it.syncTag ?: "row-${it.id}",
                                occurrences = occurrencesFor(it),
                                reminderMinutes = it.reminderMinutes
                            )
                        },
                        fileName = name,
                        localStrings = localStrings
                    )
                }
                _events.send(UiEvent.Share(intent, localStrings.exportIcs))
            } catch (e: Exception) {
                Log.e(TAG, "ICS export failed", e)
                _events.send(UiEvent.Message(localStrings.exportFailed))
            }
        }
    }

    private suspend fun buildShareIntent(
        calendarName: String,
        events: List<IcsEvent>,
        fileName: String,
        localStrings: AppStrings
    ): Intent = withContext(Dispatchers.IO) {
        val ics = IcsExporter.generateIcs(
            calendarName = calendarName,
            events = events,
            labels = syncLabels(),
            noteFor = { localStrings.noteText(it) }
        )
        IcsExporter.createShareIntent(getApplication(), fileName, ics)
    }
}
