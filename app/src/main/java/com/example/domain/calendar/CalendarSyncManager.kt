package com.example.domain.calendar

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.domain.model.CalculatedOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

class CalendarSyncManager(private val context: Context) {

    companion object {
        const val APP_TAG_NAME = "app"
        const val APP_TAG_VALUE = "hebrew_calendar_sync"
        const val CUSTOM_EVENT_ID_NAME = "hebrew_event_id"
    }

    private val contentResolver: ContentResolver = context.contentResolver

    fun hasCalendarPermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writePerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readPerm && writePerm
    }

    suspend fun getAvailableCalendars(): List<DeviceCalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val calendars = mutableListOf<DeviceCalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idCol = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryCol = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val accessCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Calendar $id"
                    val accName = it.getString(accNameCol) ?: ""
                    val accType = it.getString(accTypeCol) ?: ""
                    val color = it.getInt(colorCol)
                    val isPrimary = if (primaryCol >= 0) it.getInt(primaryCol) == 1 else false
                    val isLocal = accType == CalendarContract.ACCOUNT_TYPE_LOCAL
                    val accessLevel = if (accessCol >= 0) it.getInt(accessCol) else CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                    // Only include calendars that are writable (access level >= CONTRIBUTOR)
                    if (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        calendars.add(
                            DeviceCalendarInfo(
                                id = id,
                                displayName = name,
                                accountName = accName,
                                accountType = accType,
                                color = color,
                                isPrimary = isPrimary,
                                isLocal = isLocal
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        calendars
    }

    /**
     * Creates a new dedicated local calendar for Hebrew Events.
     */
    suspend fun createNewHebrewCalendar(displayName: String = "אירועים עבריים (Hebrew Events)"): Long? =
        withContext(Dispatchers.IO) {
            if (!hasCalendarPermission()) return@withContext null

            // 1. Try standard LOCAL sync adapter account
            try {
                val accountName = "HebrewCalendarSyncApp"
                val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    .build()

                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    put(CalendarContract.Calendars.NAME, "HebrewCalendarSync")
                    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
                    put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF2A5298.toInt())
                    put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                    put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
                    put(CalendarContract.Calendars.VISIBLE, 1)
                    put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
                }

                val resultUri = contentResolver.insert(uri, values)
                val id = resultUri?.lastPathSegment?.toLongOrNull()
                if (id != null && id > 0L) return@withContext id
            } catch (e: Exception) {
                android.util.Log.w("CalendarSyncManager", "Local calendar insert attempt 1 failed", e)
            }

            // 2. Try using app package name as account
            try {
                val accountName = context.packageName
                val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    .build()

                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                    put(CalendarContract.Calendars.NAME, displayName)
                    put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
                    put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF1E3C72.toInt())
                    put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
                    put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
                    put(CalendarContract.Calendars.VISIBLE, 1)
                    put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
                }

                val resultUri = contentResolver.insert(uri, values)
                val id = resultUri?.lastPathSegment?.toLongOrNull()
                if (id != null && id > 0L) return@withContext id
            } catch (e: Exception) {
                android.util.Log.w("CalendarSyncManager", "Local calendar insert attempt 2 failed", e)
            }

            null
        }

    /**
     * Injects calculated occurrences as All-Day events into the chosen calendar.
     * Events are tagged using ExtendedProperties or formatted description fallback so
     * events are reliably inserted into Google Calendar and device calendars.
     * Returns count of successfully inserted events.
     */
    suspend fun insertEvents(
        calendarId: Long,
        eventTitle: String,
        occurrences: List<CalculatedOccurrence>,
        customEventId: String? = null
    ): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission() || occurrences.isEmpty()) return@withContext 0

        var insertedCount = 0
        // Process in batches of 20
        val chunkSize = 20
        val chunks = occurrences.chunked(chunkSize)

        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

        for (chunk in chunks) {
            val operations = ArrayList<ContentProviderOperation>()

            for (occ in chunk) {
                utcCalendar.clear()
                utcCalendar.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
                val startMillis = utcCalendar.timeInMillis
                val endMillis = startMillis + (24 * 60 * 60 * 1000L) // 1 full day

                val desc = buildString {
                    append("תאריך עברי: ")
                    append(occ.hebrewDateFormatted)
                    if (!occ.note.isNullOrBlank()) {
                        append(" (")
                        append(occ.note)
                        append(")")
                    }
                    append("\n[$APP_TAG_NAME:$APP_TAG_VALUE]")
                    if (!customEventId.isNullOrBlank()) {
                        append("\n[$CUSTOM_EVENT_ID_NAME:$customEventId]")
                    }
                    append("\nנוצר באמצעות Hebrew Calendar Sync")
                }

                val eventBackRefIndex = operations.size

                val eventOp = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                    .withValue(CalendarContract.Events.TITLE, eventTitle)
                    .withValue(CalendarContract.Events.DESCRIPTION, desc)
                    .withValue(CalendarContract.Events.ALL_DAY, 1)
                    .withValue(CalendarContract.Events.DTSTART, startMillis)
                    .withValue(CalendarContract.Events.DTEND, endMillis)
                    .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                    .withValue(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
                    .build()

                operations.add(eventOp)

                // Tag event using ExtendedProperties: name="app", value="hebrew_calendar_sync"
                val propOp = ContentProviderOperation.newInsert(CalendarContract.ExtendedProperties.CONTENT_URI)
                    .withValueBackReference(CalendarContract.ExtendedProperties.EVENT_ID, eventBackRefIndex)
                    .withValue(CalendarContract.ExtendedProperties.NAME, APP_TAG_NAME)
                    .withValue(CalendarContract.ExtendedProperties.VALUE, APP_TAG_VALUE)
                    .build()

                operations.add(propOp)

                // Tag with custom event ID if available
                if (!customEventId.isNullOrBlank()) {
                    val idPropOp = ContentProviderOperation.newInsert(CalendarContract.ExtendedProperties.CONTENT_URI)
                        .withValueBackReference(CalendarContract.ExtendedProperties.EVENT_ID, eventBackRefIndex)
                        .withValue(CalendarContract.ExtendedProperties.NAME, CUSTOM_EVENT_ID_NAME)
                        .withValue(CalendarContract.ExtendedProperties.VALUE, customEventId)
                        .build()

                    operations.add(idPropOp)
                }
            }

            try {
                val results = contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
                val count = results.count { it.uri != null }
                android.util.Log.d("CalendarSyncManager", "Batch insert with ExtendedProperties succeeded, inserted: $count")
                insertedCount += if (count > 0) chunk.size else 0
            } catch (e: Exception) {
                android.util.Log.w("CalendarSyncManager", "Batch with ExtendedProperties failed, trying pure events fallback", e)
                // Fallback 1: If ExtendedProperties is unsupported on this provider (e.g. Google Calendar), insert pure events
                try {
                    val pureOps = ArrayList<ContentProviderOperation>()
                    for (occ in chunk) {
                        utcCalendar.clear()
                        utcCalendar.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
                        val startMillis = utcCalendar.timeInMillis
                        val endMillis = startMillis + (24 * 60 * 60 * 1000L)

                        val desc = buildString {
                            append("תאריך עברי: ")
                            append(occ.hebrewDateFormatted)
                            if (!occ.note.isNullOrBlank()) {
                                append(" (")
                                append(occ.note)
                                append(")")
                            }
                            append("\n[$APP_TAG_NAME:$APP_TAG_VALUE]")
                            if (!customEventId.isNullOrBlank()) {
                                append("\n[$CUSTOM_EVENT_ID_NAME:$customEventId]")
                            }
                            append("\nנוצר באמצעות Hebrew Calendar Sync")
                        }

                        pureOps.add(
                            ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                                .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                                .withValue(CalendarContract.Events.TITLE, eventTitle)
                                .withValue(CalendarContract.Events.DESCRIPTION, desc)
                                .withValue(CalendarContract.Events.ALL_DAY, 1)
                                .withValue(CalendarContract.Events.DTSTART, startMillis)
                                .withValue(CalendarContract.Events.DTEND, endMillis)
                                .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                                .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                                .withValue(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
                                .build()
                        )
                    }
                    val fallbackResults = contentResolver.applyBatch(CalendarContract.AUTHORITY, pureOps)
                    val count = fallbackResults.count { it.uri != null }
                    android.util.Log.d("CalendarSyncManager", "Fallback pure batch succeeded, count: $count")
                    insertedCount += if (count > 0) count else chunk.size
                } catch (e2: Exception) {
                    android.util.Log.e("CalendarSyncManager", "Pure event batch insert failed, trying individual inserts", e2)
                    // Fallback 2: Insert events individually using ContentResolver.insert
                    for (occ in chunk) {
                        try {
                            utcCalendar.clear()
                            utcCalendar.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
                            val startMillis = utcCalendar.timeInMillis
                            val endMillis = startMillis + (24 * 60 * 60 * 1000L)

                            val values = ContentValues().apply {
                                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                                put(CalendarContract.Events.TITLE, eventTitle)
                                put(CalendarContract.Events.DESCRIPTION, "תאריך עברי: ${occ.hebrewDateFormatted}\n[$APP_TAG_NAME:$APP_TAG_VALUE]\nנוצר באמצעות Hebrew Calendar Sync")
                                put(CalendarContract.Events.ALL_DAY, 1)
                                put(CalendarContract.Events.DTSTART, startMillis)
                                put(CalendarContract.Events.DTEND, endMillis)
                                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
                            }
                            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                            if (uri != null) {
                                insertedCount++
                            }
                        } catch (e3: Exception) {
                            android.util.Log.e("CalendarSyncManager", "Individual event insert failed", e3)
                        }
                    }
                }
            }
        }

        insertedCount
    }

    /**
     * Deletes events matching the specified title from user's calendar(s).
     * Strictly targets events tagged by this app via ExtendedProperties (name="app", value="hebrew_calendar_sync"),
     * falling back to title match only if no tagged events are found or provider lacks extended properties.
     */
    suspend fun deleteEventsByTitle(title: String, calendarId: Long? = null): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        try {
            // First step: query event IDs tagged with our app's extended property
            val taggedEventIds = getAppTaggedEventIds(title = title, calendarId = calendarId)

            if (taggedEventIds.isNotEmpty()) {
                // Batch delete specifically tagged event IDs
                var deleted = 0
                val idChunks = taggedEventIds.chunked(40)
                for (chunk in idChunks) {
                    val placeholders = chunk.joinToString(",") { "?" }
                    val selection = "${CalendarContract.Events._ID} IN ($placeholders)"
                    val selectionArgs = chunk.map { it.toString() }.toTypedArray()
                    deleted += contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
                }
                return@withContext deleted
            }

            // Fallback for legacy events: delete by title and optional calendarId
            val selection: String
            val selectionArgs: Array<String>

            if (calendarId != null) {
                selection = "${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.CALENDAR_ID} = ?"
                selectionArgs = arrayOf(title, calendarId.toString())
            } else {
                selection = "${CalendarContract.Events.TITLE} = ?"
                selectionArgs = arrayOf(title)
            }

            contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Queries ExtendedProperties to find event IDs tagged by this app and matching the title.
     */
    private fun getAppTaggedEventIds(title: String, calendarId: Long?): Set<Long> {
        val eventIds = mutableSetOf<Long>()
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                CalendarContract.ExtendedProperties.EVENT_ID
            )
            val selection = "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?"
            val selectionArgs = arrayOf(APP_TAG_NAME, APP_TAG_VALUE)

            cursor = contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.let {
                val eventIdCol = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
                while (it.moveToNext()) {
                    eventIds.add(it.getLong(eventIdCol))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        if (eventIds.isEmpty()) return emptySet()

        // Filter event IDs to verify title and calendarId
        val verifiedIds = mutableSetOf<Long>()
        val chunks = eventIds.toList().chunked(40)
        for (chunk in chunks) {
            var eventCursor: Cursor? = null
            try {
                val placeholders = chunk.joinToString(",") { "?" }
                val where: String
                val args: Array<String>

                if (calendarId != null) {
                    where = "${CalendarContract.Events._ID} IN ($placeholders) AND ${CalendarContract.Events.TITLE} = ? AND ${CalendarContract.Events.CALENDAR_ID} = ?"
                    args = (chunk.map { it.toString() } + title + calendarId.toString()).toTypedArray()
                } else {
                    where = "${CalendarContract.Events._ID} IN ($placeholders) AND ${CalendarContract.Events.TITLE} = ?"
                    args = (chunk.map { it.toString() } + title).toTypedArray()
                }

                eventCursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID),
                    where,
                    args,
                    null
                )

                eventCursor?.let {
                    val idCol = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                    while (it.moveToNext()) {
                        verifiedIds.add(it.getLong(idCol))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                eventCursor?.close()
            }
        }

        return verifiedIds
    }

    /**
     * Deletes all events in a specific calendar.
     */
    suspend fun clearCalendar(calendarId: Long): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        try {
            contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
