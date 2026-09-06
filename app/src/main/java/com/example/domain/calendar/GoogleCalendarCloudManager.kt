package com.example.domain.calendar

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.example.domain.model.CalculatedOccurrence
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

data class GoogleCloudCalendar(
    val id: String,
    val summary: String,
    val description: String? = null,
    val timeZone: String? = null,
    val isPrimary: Boolean = false
)

/**
 * Google Calendar REST API (v3) over plain HttpURLConnection — no heavy client library.
 *
 * Two things shape this class:
 *
 * Every event carries its owning app event's tag in `extendedProperties.private`, which is
 * queryable via the `privateExtendedProperty` parameter. That is what makes [deleteEventsBySyncTag]
 * possible: without it, events written to the cloud could never be found again, and deleting an
 * event in the app would leave its occurrences in the user's Google Calendar forever.
 *
 * Writing a century of occurrences means hundreds of requests, which is exactly the shape of
 * traffic Google rate-limits. Every call goes through [request], which retries 429/5xx and
 * rate-limit 403s with exponential backoff.
 */
class GoogleCalendarCloudManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleCalendarCloud"
        const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
        private const val BASE_URL = "https://www.googleapis.com/calendar/v3"
        private const val TIMEOUT_MS = 20_000

        /** extendedProperties.private keys. */
        internal const val PROP_SYNC_ID = "hebrewSyncId"
        private const val PROP_APP = "hebrewCalendarSync"
        private const val APP_MARKER = "true"

        private const val MAX_ATTEMPTS = 5
        private const val CONCURRENCY = 6
    }

    internal data class HttpResult(val code: Int, val body: String) {
        val isSuccess get() = code in 200..299
        /** 404 on delete means it is already gone, which is the state we wanted. */
        val isGone get() = code == 404 || code == 410
    }

    // ---------------------------------------------------------------- transport

    private fun execute(method: String, urlStr: String, token: String, jsonBody: String?): HttpResult {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (jsonBody != null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return HttpResult(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            conn.disconnect()
        }
    }

    /** True for the transient failures worth retrying rather than surfacing. */
    internal fun isRetryable(result: HttpResult): Boolean = when {
        result.code == 429 -> true
        result.code in 500..599 -> true
        // Calendar reports quota exhaustion as 403 with a specific reason, unlike a real denial.
        result.code == 403 &&
            ("rateLimitExceeded" in result.body || "userRateLimitExceeded" in result.body ||
                "quotaExceeded" in result.body) -> true
        else -> false
    }

    private suspend fun request(
        method: String,
        urlStr: String,
        token: String,
        jsonBody: String? = null
    ): HttpResult {
        var lastError: Exception? = null
        for (attempt in 0 until MAX_ATTEMPTS) {
            if (attempt > 0) {
                // Exponential backoff with jitter, so parallel workers do not retry in lockstep.
                val backoff = (1L shl (attempt - 1)) * 500L + Random.nextLong(250)
                delay(backoff)
            }
            try {
                val result = execute(method, urlStr, token, jsonBody)
                if (!isRetryable(result)) return result
                Log.w(TAG, "$method $urlStr -> ${result.code}, retrying (attempt ${attempt + 1})")
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "$method $urlStr threw, retrying (attempt ${attempt + 1})", e)
            }
        }
        lastError?.let { throw it }
        return HttpResult(429, "exhausted $MAX_ATTEMPTS attempts")
    }

    // ---------------------------------------------------------------- auth

    suspend fun getAccessToken(account: Account): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(GoogleAuthUtil.getToken(context, account, "oauth2:$CALENDAR_SCOPE"))
        } catch (recoverable: UserRecoverableAuthException) {
            // Caller must launch recoverable.intent to show the consent screen.
            Result.failure(recoverable)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining access token", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------- calendars

    /**
     * Returns the caller's calendar with this exact name, creating it only if absent.
     *
     * Creating unconditionally meant a new "Hebrew Events" calendar on every sync.
     */
    suspend fun findOrCreateCalendar(
        accessToken: String,
        summary: String,
        description: String,
        timeZone: String = TimeZone.getDefault().id
    ): Result<GoogleCloudCalendar> = withContext(Dispatchers.IO) {
        listCalendars(accessToken).getOrNull()
            ?.firstOrNull { it.summary == summary }
            ?.let {
                Log.d(TAG, "Reusing existing cloud calendar ${it.id}")
                return@withContext Result.success(it)
            }
        createCalendar(accessToken, summary, description, timeZone)
    }

    suspend fun createCalendar(
        accessToken: String,
        summary: String,
        description: String,
        timeZone: String = TimeZone.getDefault().id
    ): Result<GoogleCloudCalendar> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("summary", summary)
                put("description", description)
                put("timeZone", timeZone)
            }
            val result = request("POST", "$BASE_URL/calendars", accessToken, body.toString())
            if (!result.isSuccess) {
                Log.e(TAG, "Failed to create calendar: ${result.code} ${result.body}")
                return@withContext Result.failure(Exception("HTTP ${result.code}"))
            }
            val json = JSONObject(result.body)
            Result.success(
                GoogleCloudCalendar(
                    id = json.getString("id"),
                    summary = json.optString("summary", summary),
                    description = json.optString("description", description),
                    timeZone = json.optString("timeZone", timeZone)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating calendar", e)
            Result.failure(e)
        }
    }

    /** All of the user's calendars, following pageToken to the end. */
    suspend fun listCalendars(accessToken: String): Result<List<GoogleCloudCalendar>> =
        withContext(Dispatchers.IO) {
            try {
                val all = mutableListOf<GoogleCloudCalendar>()
                var pageToken: String? = null
                do {
                    val url = buildString {
                        append("$BASE_URL/users/me/calendarList?maxResults=250")
                        pageToken?.let { append("&pageToken=").append(encode(it)) }
                    }
                    val result = request("GET", url, accessToken)
                    if (!result.isSuccess) {
                        return@withContext Result.failure(Exception("HTTP ${result.code}"))
                    }
                    val json = JSONObject(result.body)
                    json.optJSONArray("items")?.let { items ->
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            all += GoogleCloudCalendar(
                                id = item.getString("id"),
                                summary = item.optString("summary", ""),
                                description = item.optString("description", null),
                                timeZone = item.optString("timeZone", null),
                                isPrimary = item.optBoolean("primary", false)
                            )
                        }
                    }
                    pageToken = json.optString("nextPageToken", "").ifBlank { null }
                } while (pageToken != null)
                Result.success(all)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ---------------------------------------------------------------- events

    suspend fun insertEvents(
        accessToken: String,
        calendarId: String,
        eventTitle: String,
        occurrences: List<CalculatedOccurrence>,
        syncTag: String,
        labels: SyncLabels,
        reminderMinutes: Int? = null,
        noteFor: (CalculatedOccurrence) -> String? = { null }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/calendars/${encode(calendarId)}/events"
            var inserted = 0
            for (chunk in occurrences.chunked(CONCURRENCY)) {
                inserted += chunk.map { occ ->
                    async {
                        val body = eventJson(eventTitle, occ, syncTag, labels, reminderMinutes, noteFor(occ))
                        try {
                            if (request("POST", url, accessToken, body).isSuccess) 1 else 0
                        } catch (e: Exception) {
                            Log.e(TAG, "Event insert failed for ${occ.gregorianDateFormatted}", e)
                            0
                        }
                    }
                }.awaitAll().sum()
            }
            Log.d(TAG, "Cloud sync inserted $inserted/${occurrences.size}")
            Result.success(inserted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed inserting cloud events", e)
            Result.failure(e)
        }
    }

    /**
     * Removes every cloud event tagged with [syncTag].
     *
     * Matches on `extendedProperties.private`, never on the title, so this cannot touch events the
     * user created themselves.
     */
    suspend fun deleteEventsBySyncTag(
        accessToken: String,
        calendarId: String,
        syncTag: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val ids = mutableListOf<String>()
            var pageToken: String? = null
            do {
                val url = buildString {
                    append("$BASE_URL/calendars/${encode(calendarId)}/events")
                    append("?privateExtendedProperty=").append(encode("$PROP_SYNC_ID=$syncTag"))
                    append("&maxResults=250&showDeleted=false&singleEvents=true")
                    pageToken?.let { append("&pageToken=").append(encode(it)) }
                }
                val result = request("GET", url, accessToken)
                if (!result.isSuccess) {
                    return@withContext Result.failure(Exception("HTTP ${result.code}"))
                }
                val json = JSONObject(result.body)
                json.optJSONArray("items")?.let { items ->
                    for (i in 0 until items.length()) ids += items.getJSONObject(i).getString("id")
                }
                pageToken = json.optString("nextPageToken", "").ifBlank { null }
            } while (pageToken != null)

            var deleted = 0
            for (chunk in ids.chunked(CONCURRENCY)) {
                deleted += chunk.map { id ->
                    async {
                        val url = "$BASE_URL/calendars/${encode(calendarId)}/events/${encode(id)}"
                        try {
                            val r = request("DELETE", url, accessToken)
                            if (r.isSuccess || r.isGone) 1 else 0
                        } catch (e: Exception) {
                            Log.e(TAG, "Event delete failed for $id", e)
                            0
                        }
                    }
                }.awaitAll().sum()
            }
            Log.d(TAG, "Deleted $deleted/${ids.size} cloud events for tag $syncTag")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting cloud events", e)
            Result.failure(e)
        }
    }

    internal fun eventJson(
        title: String,
        occ: CalculatedOccurrence,
        syncTag: String,
        labels: SyncLabels,
        reminderMinutes: Int?,
        note: String?
    ): String {
        // Locale.US: a locale with non-ASCII digits would emit a date the API rejects.
        val start = String.format(
            Locale.US, "%04d-%02d-%02d",
            occ.gregorianYear, occ.gregorianMonth, occ.gregorianDay
        )
        val endCal = Calendar.getInstance().apply {
            clear()
            set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val end = String.format(
            Locale.US, "%04d-%02d-%02d",
            endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.DAY_OF_MONTH)
        )

        val description = buildString {
            append(labels.hebrewDateLabel).append(": ").append(occ.hebrewDateFormatted)
            if (!note.isNullOrBlank()) append('\n').append(note)
            append('\n').append(labels.createdBy)
        }

        return JSONObject().apply {
            put("summary", title)
            put("description", description)
            put("start", JSONObject().put("date", start))
            put("end", JSONObject().put("date", end))
            put("transparency", "transparent")
            put("status", "confirmed")
            // The handle that makes these events findable, and therefore deletable.
            put(
                "extendedProperties",
                JSONObject().put(
                    "private",
                    JSONObject().put(PROP_SYNC_ID, syncTag).put(PROP_APP, APP_MARKER)
                )
            )
            if (reminderMinutes != null) {
                put(
                    "reminders",
                    JSONObject()
                        .put("useDefault", false)
                        .put(
                            "overrides",
                            JSONArray().put(
                                JSONObject().put("method", "popup").put("minutes", reminderMinutes)
                            )
                        )
                )
            }
        }.toString()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
