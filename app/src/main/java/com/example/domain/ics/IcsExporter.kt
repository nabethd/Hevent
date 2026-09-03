package com.example.domain.ics

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.domain.model.CalculatedOccurrence
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IcsExporter {

    /**
     * Generates RFC 5545 compliant iCalendar (.ics) content for calculated occurrences.
     * Uses custom UIDs so imported events are uniquely tagged and distinguishable.
     */
    fun generateIcsContent(
        calendarName: String = "לוח אירועים עברי",
        eventTitle: String,
        occurrences: List<CalculatedOccurrence>,
        customEventId: String? = null
    ): String {
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Google AI Studio//Hebrew Calendar Sync 1.0//HE\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:").append(sanitizeIcsText(calendarName)).append("\r\n")
        sb.append("X-WR-TIMEZONE:UTC\r\n")

        val cal = Calendar.getInstance()
        val safePrefix = customEventId?.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            ?: eventTitle.hashCode().toString().replace("-", "n")

        for (occ in occurrences) {
            val startStr = String.format(Locale.US, "%04d%02d%02d", occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay)

            // DTEND for all-day events is next day per RFC 5545
            cal.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endStr = String.format(
                Locale.US,
                "%04d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )

            // Distinct custom UID incorporating app domain, custom ID, Hebrew date & occurrence
            val uid = "hebrew-event-${safePrefix}-${occ.targetHebrewYear}-${occ.targetHebrewMonth}-${occ.targetHebrewDay}-${occ.occurrenceIndex}@aistudio"

            val desc = buildString {
                append("תאריך עברי: ")
                append(occ.hebrewDateFormatted)
                if (!occ.note.isNullOrBlank()) {
                    append(" (").append(occ.note).append(")")
                }
                append(" | שנת מחזור: ").append(occ.occurrenceIndex)
                append(" | נוצר באמצעות Hebrew Calendar Sync")
            }

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:").append(uid).append("\r\n")
            sb.append("DTSTAMP:").append(now).append("\r\n")
            sb.append("DTSTART;VALUE=DATE:").append(startStr).append("\r\n")
            sb.append("DTEND;VALUE=DATE:").append(endStr).append("\r\n")
            sb.append("SUMMARY:").append(sanitizeIcsText(eventTitle)).append("\r\n")
            sb.append("DESCRIPTION:").append(sanitizeIcsText(desc)).append("\r\n")
            sb.append("STATUS:CONFIRMED\r\n")
            sb.append("TRANSP:TRANSPARENT\r\n")
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * Generates RFC 5545 iCalendar content for multiple events.
     * Uses custom UIDs to allow external calendar systems to track recurring occurrences.
     */
    fun generateCombinedIcs(
        calendarName: String = "אירועים עבריים מסונכרנים",
        events: List<Pair<String, List<CalculatedOccurrence>>>
    ): String {
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Google AI Studio//Hebrew Calendar Sync 1.0//HE\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:").append(sanitizeIcsText(calendarName)).append("\r\n")

        val cal = Calendar.getInstance()

        for ((title, occurrences) in events) {
            val titleSlug = title.hashCode().toString().replace("-", "n")
            for (occ in occurrences) {
                val startStr = String.format(Locale.US, "%04d%02d%02d", occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay)
                cal.set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay)
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val endStr = String.format(
                    Locale.US,
                    "%04d%02d%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )

                // Custom UID per event and occurrence
                val uid = "hebrew-event-${titleSlug}-${occ.targetHebrewYear}-${occ.targetHebrewMonth}-${occ.targetHebrewDay}-${occ.occurrenceIndex}@aistudio"
                val desc = "תאריך עברי: ${occ.hebrewDateFormatted}" +
                        (if (occ.note != null) " (${occ.note})" else "") +
                        " | שנת מחזור: ${occ.occurrenceIndex} | נוצר באמצעות Hebrew Calendar Sync"

                sb.append("BEGIN:VEVENT\r\n")
                sb.append("UID:").append(uid).append("\r\n")
                sb.append("DTSTAMP:").append(now).append("\r\n")
                sb.append("DTSTART;VALUE=DATE:").append(startStr).append("\r\n")
                sb.append("DTEND;VALUE=DATE:").append(endStr).append("\r\n")
                sb.append("SUMMARY:").append(sanitizeIcsText(title)).append("\r\n")
                sb.append("DESCRIPTION:").append(sanitizeIcsText(desc)).append("\r\n")
                sb.append("STATUS:CONFIRMED\r\n")
                sb.append("TRANSP:TRANSPARENT\r\n")
                sb.append("END:VEVENT\r\n")
            }
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * Writes ICS content to a temporary cache file and creates a share intent.
     */
    fun createShareIntent(context: Context, fileName: String, icsContent: String): Intent {
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9א-ת_\\-]"), "_") + ".ics"
        val exportDir = File(context.cacheDir, "calendar_exports").apply { mkdirs() }
        val file = File(exportDir, safeFileName)
        file.writeText(icsContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun sanitizeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
    }
}
