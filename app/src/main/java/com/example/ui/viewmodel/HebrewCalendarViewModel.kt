package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.HebrewEventEntity
import com.example.data.repository.HebrewEventRepository
import com.example.domain.calendar.CalendarSyncManager
import com.example.domain.calendar.DeviceCalendarInfo
import com.example.domain.hebrew.HebrewCalendarEngine
import com.example.domain.ics.IcsExporter
import com.example.domain.model.CalculatedOccurrence
import com.example.domain.model.HebrewDateInfo
import com.example.domain.model.LeapYearRule
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class HebrewCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HebrewEventRepository
    val calendarSyncManager: CalendarSyncManager = CalendarSyncManager(application)

    init {
        val db = AppDatabase.getInstance(application)
        repository = HebrewEventRepository(db.hebrewEventDao())
    }

    // Language state - Hebrew by default per user request
    private val _language = MutableStateFlow(AppLanguage.HEBREW)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val strings: AppStrings
        get() = AppStrings(_language.value)

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
    }

    // Database events
    val events: StateFlow<List<HebrewEventEntity>> = repository.allEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Calendar sync state
    private val _calendars = MutableStateFlow<List<DeviceCalendarInfo>>(emptyList())
    val calendars: StateFlow<List<DeviceCalendarInfo>> = _calendars.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(calendarSyncManager.hasCalendarPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Calendar View Navigation
    private val todayCal = Calendar.getInstance()
    private val _calViewYear = MutableStateFlow(todayCal.get(Calendar.YEAR))
    val calViewYear: StateFlow<Int> = _calViewYear.asStateFlow()

    private val _calViewMonth = MutableStateFlow(todayCal.get(Calendar.MONTH) + 1)
    val calViewMonth: StateFlow<Int> = _calViewMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow(todayCal.get(Calendar.DAY_OF_MONTH))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    init {
        refreshCalendarPermissions()
    }

    fun refreshCalendarPermissions() {
        val hasPerm = calendarSyncManager.hasCalendarPermission()
        _hasCalendarPermission.value = hasPerm
        if (hasPerm) {
            loadDeviceCalendars()
        }
    }

    fun loadDeviceCalendars() {
        viewModelScope.launch {
            val list = calendarSyncManager.getAvailableCalendars()
            _calendars.value = list
        }
    }

    fun setCalendarMonth(year: Int, month: Int) {
        _calViewYear.value = year
        _calViewMonth.value = month
    }

    fun prevCalendarMonth() {
        var y = _calViewYear.value
        var m = _calViewMonth.value - 1
        if (m < 1) {
            m = 12
            y--
        }
        _calViewYear.value = y
        _calViewMonth.value = m
    }

    fun nextCalendarMonth() {
        var y = _calViewYear.value
        var m = _calViewMonth.value + 1
        if (m > 12) {
            m = 1
            y++
        }
        _calViewYear.value = y
        _calViewMonth.value = m
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Creates a new dedicated Hebrew events calendar on the device.
     */
    suspend fun createNewHebrewCalendar(name: String): Long? {
        val id = calendarSyncManager.createNewHebrewCalendar(name)
        if (id != null) {
            loadDeviceCalendars()
        }
        return id
    }

    /**
     * Item representation for single or batch event creation.
     */
    data class EventDraft(
        val title: String,
        val eventType: String,
        val recurrenceType: String,
        val hebrewDateInfo: HebrewDateInfo,
        val leapYearRule: LeapYearRule = LeapYearRule.STANDARD_ADAR_II,
        val yearsCount: Int = 20
    )

    /**
     * Adds an event, calculates occurrences based on chosen yearsCount,
     * saves to Room, and optionally syncs to device calendar.
     */
    fun saveEvent(
        title: String,
        eventType: String,
        recurrenceType: String,
        hebrewDateInfo: HebrewDateInfo,
        leapYearRule: LeapYearRule,
        yearsCount: Int = 20,
        targetCalendarId: Long?,
        targetCalendarName: String?,
        isIcsOnly: Boolean,
        onComplete: (Boolean, Intent?) -> Unit
    ) {
        saveBatchEvents(
            events = listOf(
                EventDraft(
                    title = title,
                    eventType = eventType,
                    recurrenceType = recurrenceType,
                    hebrewDateInfo = hebrewDateInfo,
                    leapYearRule = leapYearRule,
                    yearsCount = yearsCount
                )
            ),
            targetCalendarId = targetCalendarId,
            targetCalendarName = targetCalendarName,
            isIcsOnly = isIcsOnly,
            onComplete = onComplete
        )
    }

    /**
     * Adds multiple events in a single batch, syncing them together to the target calendar or ICS.
     */
    fun saveBatchEvents(
        events: List<EventDraft>,
        targetCalendarId: Long?,
        targetCalendarName: String?,
        isIcsOnly: Boolean,
        onComplete: (Boolean, Intent?) -> Unit
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                var totalSynced = 0
                val allOccurrencesWithTitle = mutableListOf<Pair<String, List<CalculatedOccurrence>>>()

                for (draft in events) {
                    val count = if (draft.yearsCount > 0) draft.yearsCount else 20
                    val occurrences = if (draft.recurrenceType == "MONTHLY") {
                        HebrewCalendarEngine.calculateMonthlyOccurrences(
                            originHebrewDay = draft.hebrewDateInfo.hebrewDay,
                            monthsCount = count * 12
                        )
                    } else {
                        HebrewCalendarEngine.calculateYearlyOccurrences(
                            originHebrewYear = draft.hebrewDateInfo.hebrewYear,
                            originHebrewMonth = draft.hebrewDateInfo.hebrewMonth,
                            originHebrewDay = draft.hebrewDateInfo.hebrewDay,
                            leapYearRule = draft.leapYearRule,
                            yearsCount = count,
                            startFromCurrentYear = true
                        )
                    }

                    val customEventId = "hevent_${draft.hebrewDateInfo.hebrewYear}_${draft.hebrewDateInfo.hebrewMonth}_${draft.hebrewDateInfo.hebrewDay}_${System.currentTimeMillis()}"

                    var syncedCount = 0
                    if (!isIcsOnly && targetCalendarId != null && calendarSyncManager.hasCalendarPermission()) {
                        syncedCount = calendarSyncManager.insertEvents(
                            calendarId = targetCalendarId,
                            eventTitle = draft.title,
                            occurrences = occurrences,
                            customEventId = customEventId
                        )
                        totalSynced += syncedCount
                    }

                    val entity = HebrewEventEntity(
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
                        leapYearRule = draft.leapYearRule.id,
                        yearsCount = occurrences.size,
                        targetCalendarId = if (!isIcsOnly) targetCalendarId else null,
                        targetCalendarName = if (!isIcsOnly) targetCalendarName else null,
                        isSyncedToCalendar = syncedCount > 0,
                        syncedEventsCount = syncedCount
                    )
                    repository.insertEvent(entity)
                    allOccurrencesWithTitle.add(draft.title to occurrences)
                }

                var shareIntent: Intent? = null
                if (isIcsOnly && allOccurrencesWithTitle.isNotEmpty()) {
                    val combinedIcs = IcsExporter.generateCombinedIcs(
                        calendarName = if (events.size == 1) events[0].title else "אירועים עבריים",
                        events = allOccurrencesWithTitle
                    )

                    shareIntent = IcsExporter.createShareIntent(
                        context = getApplication(),
                        fileName = if (events.size == 1) events[0].title else "hebrew_events_batch",
                        icsContent = combinedIcs
                    )
                    _statusMessage.value = strings.icsExportReady
                } else {
                    _statusMessage.value = if (totalSynced > 0) strings.syncSuccess else null
                }

                onComplete(true, shareIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = e.localizedMessage
                onComplete(false, null)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Deletes an event from the database and removes all its occurrences from the device calendar.
     */
    fun deleteEvent(event: HebrewEventEntity) {
        viewModelScope.launch {
            if (calendarSyncManager.hasCalendarPermission()) {
                calendarSyncManager.deleteEventsByTitle(event.title, event.targetCalendarId)
            }
            repository.deleteEvent(event)
            _statusMessage.value = strings.deleteSuccess
        }
    }

    /**
     * Bulk deletes all events matching a given title from device calendar and database.
     */
    fun deleteEventsByName(name: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            var deletedFromCal = 0
            if (calendarSyncManager.hasCalendarPermission()) {
                deletedFromCal = calendarSyncManager.deleteEventsByTitle(name)
            }
            val deletedFromDb = repository.deleteEventsByTitle(name)
            _statusMessage.value = "$deletedFromCal ${strings.eventsDeletedFromCal}"
            onResult(deletedFromCal + deletedFromDb)
        }
    }

    /**
     * Generates a shareable ICS intent for a single event.
     */
    fun exportEventToIcs(event: HebrewEventEntity): Intent {
        val occurrences = if (event.recurrenceType == "MONTHLY") {
            HebrewCalendarEngine.calculateMonthlyOccurrences(
                originHebrewDay = event.hebrewDay,
                monthsCount = 120
            )
        } else {
            HebrewCalendarEngine.calculateYearlyOccurrences(
                originHebrewYear = event.hebrewYear,
                originHebrewMonth = event.hebrewMonth,
                originHebrewDay = event.hebrewDay,
                leapYearRule = LeapYearRule.valueOf(event.leapYearRule),
                yearsCount = event.yearsCount,
                startFromCurrentYear = true
            )
        }

        val ics = IcsExporter.generateIcsContent(
            calendarName = "לוח עברי - ${event.title}",
            eventTitle = event.title,
            occurrences = occurrences,
            customEventId = "hevent_${event.id}_${event.hebrewYear}_${event.hebrewMonth}_${event.hebrewDay}"
        )

        return IcsExporter.createShareIntent(
            context = getApplication(),
            fileName = event.title,
            icsContent = ics
        )
    }

    /**
     * Generates a combined ICS file containing all scheduled events in the database.
     */
    fun exportAllEventsToIcs(eventsList: List<HebrewEventEntity>): Intent {
        val pairs = eventsList.map { event ->
            val occs = if (event.recurrenceType == "MONTHLY") {
                HebrewCalendarEngine.calculateMonthlyOccurrences(
                    originHebrewDay = event.hebrewDay,
                    monthsCount = 120
                )
            } else {
                HebrewCalendarEngine.calculateYearlyOccurrences(
                    originHebrewYear = event.hebrewYear,
                    originHebrewMonth = event.hebrewMonth,
                    originHebrewDay = event.hebrewDay,
                    leapYearRule = LeapYearRule.valueOf(event.leapYearRule),
                    yearsCount = event.yearsCount,
                    startFromCurrentYear = true
                )
            }
            Pair(event.title, occs)
        }

        val ics = IcsExporter.generateCombinedIcs(
            calendarName = "כל האירועים העבריים",
            events = pairs
        )

        return IcsExporter.createShareIntent(
            context = getApplication(),
            fileName = "all_hebrew_events",
            icsContent = ics
        )
    }
}
