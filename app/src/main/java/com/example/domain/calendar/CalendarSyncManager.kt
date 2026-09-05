package com.example.domain.calendar

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.model.CalculatedOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/** Language-dependent text embedded in the calendar rows we create. */
data class SyncLabels(
    val hebrewDateLabel: String,
    val createdBy: String
)

class CalendarSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "CalendarSyncManager"

        const val APP_TAG_NAME = "app"
        const val APP_TAG_VALUE = "hebrew_calendar_sync"
        const val CUSTOM_EVENT_ID_NAME = "hebrew_event_id"

        private const val BATCH_SIZE = 20
        private const val ID_CHUNK = 40

        /** Hyphens only — no `_`, which is a wildcard in SQL LIKE. */
        fun newSyncTag(): String = "hcs-" + UUID.randomUUID().toString()

        /** Escapes the LIKE metacharacters so a tag can never match more rows than intended. */
        private fun escapeLike(value: String): String =
            value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }

    private val contentResolver: ContentResolver = context.contentResolver

    fun hasCalendarPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    suspend fun getAvailableCalendars(): List<DeviceCalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        val calendars = mutableListOf<DeviceCalendarInfo>()
        try {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryCol = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val accessCol = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (cursor.moveToNext()) {
                    val accessLevel = if (accessCol >= 0) cursor.getInt(accessCol)
                    else CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue

                    val id = cursor.getLong(idCol)
                    val accType = cursor.getString(accTypeCol) ?: ""
                    calendars.add(
                        DeviceCalendarInfo(
                            id = id,
                            displayName = cursor.getString(nameCol) ?: "Calendar $id",
                            accountName = cursor.getString(accNameCol) ?: "",
                            accountType = accType,
                            color = cursor.getInt(colorCol),
                            isPrimary = if (primaryCol >= 0) cursor.getInt(primaryCol) == 1 else false,
                            isLocal = accType == CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query device calendars", e)
        }
        calendars
    }

    /** Creates a dedicated local calendar. Returns null if the device policy forbids it. */
    suspend fun createNewHebrewCalendar(displayName: String): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        // Some OEM providers reject a synthetic account name but accept the package name, so we
        // try both before giving up.
        for (accountName in listOf("HebrewCalendarSyncApp", context.packageName)) {
            try {
                val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.ACCOUNT_TYPE_LOCAL
                    )
                    .build()

                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    put(CalendarContract.Calendars.NAME, displayName)
                    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
                    put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF1A56DB.toInt())
                    put(
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                        CalendarContract.Calendars.CAL_ACCESS_OWNER
                    )
                    put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
                    put(CalendarContract.Calendars.VISIBLE, 1)
                    put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
                }

                val id = contentResolver.insert(uri, values)?.lastPathSegment?.toLongOrNull()
                if (id != null && id > 0L) return@withContext id
            } catch (e: Exception) {
                Log.w(TAG, "Local calendar insert failed for account '$accountName'", e)
            }
        }
        null
    }

    /**
     * Inserts each occurrence as a standalone all-day event, tagged with [syncTag] both as an
     * ExtendedProperty and inside the description. The description copy is the fallback for
     * providers that silently drop extended properties — without one of the two markers an event
     * could never be deleted again without also risking the user's own data.
     *
     * All-day rows must be midnight **UTC** with EVENT_TIMEZONE=UTC; anything else shifts the day
     * for users east or west of GMT.
     *
     * @return the number of rows actually created.
     */
    suspend fun insertEvents(
        calendarId: Long,
        eventTitle: String,
        occurrences: List<CalculatedOccurrence>,
        syncTag: String,
        labels: SyncLabels,
        reminderMinutes: Int? = null,
        noteFor: (CalculatedOccurrence) -> String? = { null }
    ): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission() || occurrences.isEmpty()) return@withContext 0

        var inserted = 0
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        for (chunk in occurrences.chunked(BATCH_SIZE)) {
            val ops = ArrayList<ContentProviderOperation>()
            val eventOpIndices = mutableListOf<Int>()

            for (occ in chunk) {
                val index = ops.size
                eventOpIndices.add(index)
                ops.add(
                    eventRow(calendarId, eventTitle, occ, syncTag, labels, noteFor(occ), utc)
                        .toInsertOp(hasReminder = reminderMinutes != null)
                )
                ops.add(extendedProp(index, APP_TAG_NAME, APP_TAG_VALUE))
                ops.add(extendedProp(index, CUSTOM_EVENT_ID_NAME, syncTag))
                if (reminderMinutes != null) ops.add(reminderOp(index, reminderMinutes))
            }

            try {
                val results = contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
                // Count only the event rows, not the two property rows that follow each one.
                inserted += eventOpIndices.count { it < results.size && results[it].uri != null }
            } catch (e: Exception) {
                Log.w(TAG, "Batch with extended properties failed; retrying without them", e)
                inserted += insertWithoutExtendedProperties(
                    calendarId, eventTitle, chunk, syncTag, labels, reminderMinutes, noteFor, utc
                )
            }
        }
        inserted
    }

    private fun insertWithoutExtendedProperties(
        calendarId: Long,
        eventTitle: String,
        chunk: List<CalculatedOccurrence>,
        syncTag: String,
        labels: SyncLabels,
        reminderMinutes: Int?,
        noteFor: (CalculatedOccurrence) -> String?,
        utc: Calendar
    ): Int {
        var inserted = 0
        for (occ in chunk) {
            try {
                val values = eventRow(calendarId, eventTitle, occ, syncTag, labels, noteFor(occ), utc)
                    .toContentValues(hasReminder = reminderMinutes != null)
                val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    inserted++
                    val eventId = uri.lastPathSegment?.toLongOrNull()
                    if (reminderMinutes != null && eventId != null) {
                        runCatching {
                            contentResolver.insert(
                                CalendarContract.Reminders.CONTENT_URI,
                                ContentValues().apply {
                                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                                    put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                                    put(
                                        CalendarContract.Reminders.METHOD,
                                        CalendarContract.Reminders.METHOD_ALERT
                                    )
                                }
                            )
                        }.onFailure { Log.w(TAG, "Reminder insert failed", it) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Individual event insert failed", e)
            }
        }
        return inserted
    }

    /** One resolved calendar row: the values are identical for the batch and single-insert paths. */
    private data class EventRow(
        val calendarId: Long,
        val title: String,
        val description: String,
        val startMillis: Long,
        val endMillis: Long
    )

    private fun eventRow(
        calendarId: Long,
        title: String,
        occ: CalculatedOccurrence,
        syncTag: String,
        labels: SyncLabels,
        note: String?,
        utc: Calendar
    ): EventRow {
        utc.clear()
        utc.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
        val start = utc.timeInMillis

        val description = buildString {
            append(labels.hebrewDateLabel).append(": ").append(occ.hebrewDateFormatted)
            if (!note.isNullOrBlank()) append(" (").append(note).append(")")
            append("\n[").append(APP_TAG_NAME).append(':').append(APP_TAG_VALUE).append(']')
            append("\n[").append(CUSTOM_EVENT_ID_NAME).append(':').append(syncTag).append(']')
            append('\n').append(labels.createdBy)
        }

        return EventRow(
            calendarId = calendarId,
            title = title,
            description = description,
            startMillis = start,
            endMillis = start + 24L * 60 * 60 * 1000
        )
    }

    private fun EventRow.toInsertOp(hasReminder: Boolean): ContentProviderOperation =
        ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
            .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
            .withValue(CalendarContract.Events.TITLE, title)
            .withValue(CalendarContract.Events.DESCRIPTION, description)
            .withValue(CalendarContract.Events.ALL_DAY, 1)
            .withValue(CalendarContract.Events.DTSTART, startMillis)
            .withValue(CalendarContract.Events.DTEND, endMillis)
            .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            .withValue(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
            .withValue(CalendarContract.Events.HAS_ALARM, if (hasReminder) 1 else 0)
            .build()

    private fun EventRow.toContentValues(hasReminder: Boolean): ContentValues = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.ALL_DAY, 1)
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, endMillis)
        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
        put(CalendarContract.Events.HAS_ALARM, if (hasReminder) 1 else 0)
    }

    private fun reminderOp(eventBackRef: Int, minutes: Int): ContentProviderOperation =
        ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventBackRef)
            .withValue(CalendarContract.Reminders.MINUTES, minutes)
            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            .build()

    private fun extendedProp(backRef: Int, name: String, value: String): ContentProviderOperation =
        ContentProviderOperation.newInsert(CalendarContract.ExtendedProperties.CONTENT_URI)
            .withValueBackReference(CalendarContract.ExtendedProperties.EVENT_ID, backRef)
            .withValue(CalendarContract.ExtendedProperties.NAME, name)
            .withValue(CalendarContract.ExtendedProperties.VALUE, value)
            .build()

    /**
     * Removes the calendar rows this app created for one event.
     *
     * Matching is deliberately narrow, in this order:
     *   1. rows carrying our ExtendedProperty for [syncTag];
     *   2. rows whose description contains the `[hebrew_event_id:<tag>]` marker.
     *
     * Legacy rows written before sync tags existed fall back to the app-wide tag scoped by title
     * **and** calendar. There is deliberately no title-only path: an earlier version had one, and
     * it would happily delete identically-named events the user created themselves.
     *
     * @return the number of rows deleted.
     */
    suspend fun deleteSyncedEvents(
        syncTag: String?,
        title: String,
        calendarId: Long?
    ): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        try {
            val taggedIds = if (syncTag != null) {
                findEventIdsByExtendedProperty(CUSTOM_EVENT_ID_NAME, syncTag, calendarId, title = null)
            } else {
                findEventIdsByExtendedProperty(APP_TAG_NAME, APP_TAG_VALUE, calendarId, title = title)
            }
            if (taggedIds.isNotEmpty()) return@withContext deleteByIds(taggedIds)

            // Provider dropped our extended properties — fall back to the description marker.
            val marker = if (syncTag != null) {
                "[$CUSTOM_EVENT_ID_NAME:$syncTag]"
            } else {
                "[$APP_TAG_NAME:$APP_TAG_VALUE]"
            }
            deleteByDescriptionMarker(marker, title, calendarId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete synced events", e)
            0
        }
    }

    private fun findEventIdsByExtendedProperty(
        name: String,
        value: String,
        calendarId: Long?,
        title: String?
    ): Set<Long> {
        val candidateIds = mutableSetOf<Long>()
        try {
            contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
                "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
                arrayOf(name, value),
                null
            )?.use { cursor ->
                val col = cursor.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
                while (cursor.moveToNext()) candidateIds.add(cursor.getLong(col))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Extended property lookup unavailable on this provider", e)
            return emptySet()
        }
        if (candidateIds.isEmpty()) return emptySet()

        // Confirm each candidate still belongs to the expected calendar (and title, for legacy rows).
        val verified = mutableSetOf<Long>()
        for (chunk in candidateIds.toList().chunked(ID_CHUNK)) {
            val where = StringBuilder("${CalendarContract.Events._ID} IN (${chunk.joinToString(",") { "?" }})")
            val args = mutableListOf<String>().apply { addAll(chunk.map { it.toString() }) }
            if (title != null) {
                where.append(" AND ${CalendarContract.Events.TITLE} = ?")
                args.add(title)
            }
            if (calendarId != null) {
                where.append(" AND ${CalendarContract.Events.CALENDAR_ID} = ?")
                args.add(calendarId.toString())
            }
            try {
                contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID),
                    where.toString(),
                    args.toTypedArray(),
                    null
                )?.use { cursor ->
                    val col = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    while (cursor.moveToNext()) verified.add(cursor.getLong(col))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Event verification query failed", e)
            }
        }
        return verified
    }

    private fun deleteByIds(ids: Set<Long>): Int {
        var deleted = 0
        for (chunk in ids.toList().chunked(ID_CHUNK)) {
            deleted += contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events._ID} IN (${chunk.joinToString(",") { "?" }})",
                chunk.map { it.toString() }.toTypedArray()
            )
        }
        return deleted
    }

    private fun deleteByDescriptionMarker(marker: String, title: String, calendarId: Long?): Int {
        val where = StringBuilder(
            "${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ? ESCAPE '\\'"
        )
        val args = mutableListOf(title, "%${escapeLike(marker)}%")
        if (calendarId != null) {
            where.append(" AND ${CalendarContract.Events.CALENDAR_ID} = ?")
            args.add(calendarId.toString())
        }
        return contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            where.toString(),
            args.toTypedArray()
        )
    }
}
