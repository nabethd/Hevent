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
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

data class GoogleCloudCalendar(
    val id: String,
    val summary: String,
    val description: String? = null,
    val timeZone: String? = null,
    val isPrimary: Boolean = false
)

/**
 * Manages Google Calendar cloud integration using the Google Calendar REST API (v3).
 * Uses standard HttpURLConnection without external heavy dependencies.
 */
class GoogleCalendarCloudManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleCalendarCloud"
        const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
        private const val BASE_URL = "https://www.googleapis.com/calendar/v3"
        private const val TIMEOUT_MS = 20000
    }

    private fun postJson(urlStr: String, accessToken: String, jsonBody: String): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Pair(code, resp)
        } finally {
            conn.disconnect()
        }
    }

    private fun getJson(urlStr: String, accessToken: String): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Pair(code, resp)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun getAccessToken(account: Account): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scopeString = "oauth2:$CALENDAR_SCOPE"
            val token = GoogleAuthUtil.getToken(context, account, scopeString)
            Result.success(token)
        } catch (recoverable: UserRecoverableAuthException) {
            Result.failure(recoverable)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining access token", e)
            Result.failure(e)
        }
    }

    suspend fun createCalendar(
        accessToken: String,
        summary: String,
        description: String = "נוצר באמצעות סנכרון לוח שנה עברי",
        timeZone: String = "Asia/Jerusalem"
    ): Result<GoogleCloudCalendar> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("summary", summary)
                put("description", description)
                put("timeZone", timeZone)
            }
            val (code, bodyStr) = postJson("$BASE_URL/calendars", accessToken, jsonBody.toString())
            if (code !in 200..299) {
                Log.e(TAG, "Failed to create calendar: code=$code, body=$bodyStr")
                return@withContext Result.failure(Exception("HTTP $code: $bodyStr"))
            }
            val json = JSONObject(bodyStr)
            val calId = json.getString("id")
            val calSummary = json.optString("summary", summary)
            val calDesc = json.optString("description", description)
            val calTz = json.optString("timeZone", timeZone)
            Log.d(TAG, "Successfully created Google Cloud Calendar: $calId ($calSummary)")
            Result.success(
                GoogleCloudCalendar(
                    id = calId,
                    summary = calSummary,
                    description = calDesc,
                    timeZone = calTz
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating calendar", e)
            Result.failure(e)
        }
    }

    suspend fun insertEvents(
        accessToken: String,
        calendarId: String,
        eventTitle: String,
        occurrences: List<CalculatedOccurrence>,
        syncTag: String? = null,
        reminderMinutes: Int? = null,
        noteFor: (CalculatedOccurrence) -> String? = { null }
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val encodedCalId = URLEncoder.encode(calendarId, "UTF-8").replace("+", "%20")
            val url = "$BASE_URL/calendars/$encodedCalId/events"
            var successCount = 0
            val chunks = occurrences.chunked(6)

            for (chunk in chunks) {
                val deferreds = chunk.map { occ ->
                    async {
                        try {
                            val startCal = Calendar.getInstance().apply {
                                set(occ.gregorianYear, occ.gregorianMonth - 1, occ.gregorianDay, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = (startCal.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_MONTH, 1)
                            }
                            val startDateStr = String.format(
                                "%04d-%02d-%02d",
                                occ.gregorianYear,
                                occ.gregorianMonth,
                                occ.gregorianDay
                            )
                            val endDateStr = String.format(
                                "%04d-%02d-%02d",
                                endCal.get(Calendar.YEAR),
                                endCal.get(Calendar.MONTH) + 1,
                                endCal.get(Calendar.DAY_OF_MONTH)
                            )

                            val descBuilder = StringBuilder()
                            descBuilder.append("תאריך עברי: ${occ.hebrewDateFormatted}\n")
                            val note = noteFor(occ)
                            if (note != null) {
                                descBuilder.append("$note\n")
                            }
                            if (syncTag != null) {
                                descBuilder.append("[hebrew_sync_id:$syncTag]\n")
                            } else {
                                descBuilder.append("[hebrew_calendar_sync]\n")
                            }
                            descBuilder.append("נוצר באמצעות סנכרון לוח שנה עברי")

                            val eventJson = JSONObject().apply {
                                put("summary", eventTitle)
                                put("description", descBuilder.toString())
                                put("start", JSONObject().put("date", startDateStr))
                                put("end", JSONObject().put("date", endDateStr))
                                put("transparency", "transparent")
                                put("status", "confirmed")

                                if (reminderMinutes != null) {
                                    val overrideList = JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("method", "popup")
                                            put("minutes", reminderMinutes)
                                        })
                                    }
                                    put("reminders", JSONObject().apply {
                                        put("useDefault", false)
                                        put("overrides", overrideList)
                                    })
                                }
                            }

                            val (code, body) = postJson(url, accessToken, eventJson.toString())
                            if (code in 200..299) 1 else {
                                Log.w(TAG, "Event insert failed: code=$code, resp=$body")
                                0
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Event insert error for occ: $occ", e)
                            0
                        }
                    }
                }
                val results = deferreds.awaitAll()
                successCount += results.sum()
            }
            Log.d(TAG, "Finished cloud event sync: $successCount / ${occurrences.size} inserted")
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed inserting cloud events", e)
            Result.failure(e)
        }
    }

    suspend fun listCalendars(accessToken: String): Result<List<GoogleCloudCalendar>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/users/me/calendarList"
            val (code, bodyStr) = getJson(url, accessToken)
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("HTTP $code: $bodyStr"))
            }
            val jsonResponse = JSONObject(bodyStr)
            val items = jsonResponse.optJSONArray("items")
            val list = mutableListOf<GoogleCloudCalendar>()
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    list.add(
                        GoogleCloudCalendar(
                            id = item.getString("id"),
                            summary = item.optString("summary", ""),
                            description = item.optString("description", null),
                            timeZone = item.optString("timeZone", null),
                            isPrimary = item.optBoolean("primary", false)
                        )
                    )
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
