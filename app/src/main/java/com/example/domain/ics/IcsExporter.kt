package com.example.domain.ics

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.domain.calendar.SyncLabels
import com.example.domain.model.CalculatedOccurrence
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One event's worth of occurrences, with the stable identity used to build its UIDs. */
data class IcsEvent(
    val title: String,
    /**
     * Stable across exports — the row's sync tag or id. The previous version derived UIDs from
     * `title.hashCode()`, so renaming an event changed every UID and re-importing produced a second
     * copy of the whole series instead of updating it.
     */
    val uidSeed: String,
    val occurrences: List<CalculatedOccurrence>
)

object IcsExporter {

    private const val EXPORT_DIR = "calendar_exports"
    private const val MAX_LINE_OCTETS = 75

    fun generateIcs(
        calendarName: String,
        events: List<IcsEvent>,
        labels: SyncLabels,
        noteFor: (CalculatedOccurrence) -> String? = { null }
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        val sb = StringBuilder()
        sb.fold("BEGIN:VCALENDAR")
        sb.fold("VERSION:2.0")
        sb.fold("PRODID:-//Hebrew Calendar Sync//EN")
        sb.fold("CALSCALE:GREGORIAN")
        sb.fold("METHOD:PUBLISH")
        sb.fold("X-WR-CALNAME:" + escapeText(calendarName))

        val cal = Calendar.getInstance()
        for (event in events) {
            val seed = event.uidSeed.replace(Regex("[^A-Za-z0-9._-]"), "-")
            for (occ in event.occurrences) {
                val start = String.format(
                    Locale.US, "%04d%02d%02d",
                    occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay
                )
                // RFC 5545: DTEND for an all-day event is the *exclusive* next day.
                cal.clear()
                cal.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay)
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = String.format(
                    Locale.US, "%04d%02d%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
                )

                val description = buildString {
                    append(labels.hebrewDateLabel).append(": ").append(occ.hebrewDateFormatted)
                    noteFor(occ)?.takeIf { it.isNotBlank() }?.let { append(" (").append(it).append(")") }
                    append(" | ").append(labels.createdBy)
                }

                sb.fold("BEGIN:VEVENT")
                sb.fold("UID:hebrew-event-$seed-${occ.targetHebrewYear}-${occ.targetHebrewMonth}-${occ.targetHebrewDay}@hebrewcalendarsync")
                sb.fold("DTSTAMP:$stamp")
                sb.fold("DTSTART;VALUE=DATE:$start")
                sb.fold("DTEND;VALUE=DATE:$end")
                sb.fold("SUMMARY:" + escapeText(event.title))
                sb.fold("DESCRIPTION:" + escapeText(description))
                sb.fold("STATUS:CONFIRMED")
                sb.fold("TRANSP:TRANSPARENT")
                sb.fold("END:VEVENT")
            }
        }

        sb.fold("END:VCALENDAR")
        return sb.toString()
    }

    /**
     * Writes the .ics into the app cache and returns a share intent for it.
     *
     * Does blocking file IO — call it off the main thread. Previous exports are cleared first;
     * they used to accumulate in the cache indefinitely.
     */
    fun createShareIntent(context: Context, fileName: String, icsContent: String): Intent {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val safeName = fileName.replace(Regex("[^A-Za-z0-9\\u0590-\\u05FF_-]"), "_")
            .trim('_')
            .ifBlank { "hebrew_events" }
        val file = File(dir, "$safeName.ics")
        file.writeText(icsContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * RFC 5545 §3.1 content line folding: no line may exceed 75 octets, and continuations begin
     * with a single space. Hebrew is two bytes per character in UTF-8, so unfolded description
     * lines here ran well past the limit and strict parsers truncated or rejected them.
     */
    private fun StringBuilder.fold(line: String) {
        var budget = MAX_LINE_OCTETS
        var used = 0
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            val charCount = Character.charCount(cp)
            val octets = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (used + octets > budget) {
                append("\r\n ")
                used = 1 // the leading space counts against the limit
                budget = MAX_LINE_OCTETS
            }
            append(line, i, i + charCount)
            used += octets
            i += charCount
        }
        append("\r\n")
    }

    /** Escapes the characters RFC 5545 reserves inside a TEXT value. */
    private fun escapeText(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
}
